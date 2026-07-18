package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.AppRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class XtraGyanViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val lessonAdapter = moshi.adapter<List<Lesson>>(Types.newParameterizedType(List::class.java, Lesson::class.java))
    private val quizAdapter = moshi.adapter(Quiz::class.java)

    // Current logged-in user state
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Loading status
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Generated Quiz State (active quiz the user is taking)
    private val _activeQuiz = MutableStateFlow<Quiz?>(null)
    val activeQuiz: StateFlow<Quiz?> = _activeQuiz.asStateFlow()

    private val _quizTopic = MutableStateFlow("")
    val quizTopic: StateFlow<String> = _quizTopic.asStateFlow()

    // Exposed Flows from Repo
    val courses: StateFlow<List<CourseEntity>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveSessions: StateFlow<List<LiveSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessageEntity>> = repository.allChatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userEnrollments = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getUserEnrollments(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load default user profile or create one on first startup
        viewModelScope.launch {
            repository.getUserById("default_user").collect { user ->
                if (user == null) {
                    val defaultUser = UserEntity(
                        id = "default_user",
                        name = "Rahul Sharma",
                        email = "rahul@xtragyan.com",
                        role = "Learner",
                        interests = "Vedic Mathematics, Astrology, Programming & AI",
                        languages = "English, Hindi"
                    )
                    repository.insertUser(defaultUser)
                    _currentUser.value = defaultUser
                } else {
                    _currentUser.value = user
                }
            }
        }

        // Prepopulate courses and live sessions if DB is empty
        viewModelScope.launch {
            courses.first { true } // wait for first emission
            if (courses.value.isEmpty()) {
                prepopulateDatabase()
            }
        }
    }

    // Role switcher
    fun switchUserRole(role: String) {
        viewModelScope.launch {
            val current = _currentUser.value ?: return@launch
            val updatedUser = if (role == "Teacher") {
                current.copy(
                    role = "Teacher",
                    skills = "Astrology, Vedic Mathematics, Sanskrit",
                    earnings = 15400.0,
                    studentCount = 42
                )
            } else {
                current.copy(
                    role = role,
                    skills = "",
                    earnings = 0.0,
                    studentCount = 0
                )
            }
            repository.insertUser(updatedUser)
            _currentUser.value = updatedUser
        }
    }

    fun updateProfile(name: String, email: String, interests: String, languages: String, skills: String = "") {
        viewModelScope.launch {
            val current = _currentUser.value ?: return@launch
            val updated = current.copy(
                name = name,
                email = email,
                interests = interests,
                languages = languages,
                skills = skills
            )
            repository.insertUser(updated)
            _currentUser.value = updated
        }
    }

    // --- Course actions ---
    fun enrollInCourse(courseId: Int) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.enrollInCourse(user.id, courseId)
        }
    }

    fun completeLesson(courseId: Int, lessonIndex: Int, totalLessons: Int) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.updateEnrollmentProgress(user.id, courseId, lessonIndex, totalLessons)
        }
    }

    fun toggleBookmark(courseId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.bookmarkCourse(user.id, courseId, isBookmarked)
        }
    }

    fun submitCourseReview(courseId: Int, rating: Float, review: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.submitCourseReview(user.id, courseId, rating, review)
        }
    }

    // --- Teacher Actions ---
    fun createCourse(
        title: String,
        description: String,
        category: String,
        price: Double,
        language: String,
        duration: String,
        lessons: List<Lesson>
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val lessonsJson = lessonAdapter.toJson(lessons) ?: "[]"
            val newCourse = CourseEntity(
                title = title,
                description = description,
                instructorId = user.id,
                instructorName = user.name,
                category = category,
                price = price,
                rating = 5.0f,
                language = language,
                duration = duration,
                lessonsJson = lessonsJson,
                enrollmentCount = 0
            )
            repository.insertCourse(newCourse)

            // Increment student count simulation
            repository.insertUser(user.copy(studentCount = user.studentCount + 1, earnings = user.earnings + price))
        }
    }

    fun scheduleLiveSession(
        title: String,
        category: String,
        startTime: Long,
        durationMinutes: Int,
        meetingLink: String,
        description: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val session = LiveSessionEntity(
                title = title,
                instructorName = user.name,
                category = category,
                startTime = startTime,
                durationMinutes = durationMinutes,
                meetingLink = meetingLink,
                description = description
            )
            repository.insertSession(session)
        }
    }

    // --- AI Chat Actions ---
    fun sendMessageToAi(text: String) {
        viewModelScope.launch {
            repository.addChatMessage("user", text)
            _isAiLoading.value = true

            // Ask Gemini API
            val response = repository.askGemini(
                prompt = text,
                chatHistory = chatHistory.value,
                systemInstruction = "You are the XtraGyan AI Tutor. You explain concepts related to Indian heritage, Skills development, Astrology, Vedic Mathematics, Languages, Yoga, and technology. Be encouraging, precise, and educational. Suggest practical exercises or summaries when asked."
            )
            
            repository.addChatMessage("ai", response)
            _isAiLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    // Generate Custom interactive Quiz using Gemini
    fun generateQuiz(topic: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _activeQuiz.value = null
            _quizTopic.value = topic
            
            val prompt = """
                Generate a 3-question multiple choice quiz about "${topic}". 
                Respond ONLY with a valid JSON object in this exact format, with no markdown code block backticks around it:
                {
                  "questions": [
                    {
                      "question": "What is...",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correctAnswerIndex": 0
                    }
                  ]
                }
                Make the questions challenging, educational, and accurate. Do not write any explanation outside the JSON.
            """.trimIndent()

            val response = repository.askGemini(prompt = prompt, systemInstruction = "You generate strict JSON quizzes.")
            
            // Try to parse the JSON response
            val parsedQuiz = withContext(Dispatchers.Default) {
                try {
                    // Clean response if Gemini included markdown code blocks
                    val cleanResponse = response.replace("```json", "").replace("```", "").trim()
                    quizAdapter.fromJson(cleanResponse)
                } catch (e: Exception) {
                    null
                }
            }

            if (parsedQuiz != null && parsedQuiz.questions.isNotEmpty()) {
                _activeQuiz.value = parsedQuiz
                repository.addChatMessage("ai", "I have generated an interactive 3-question quiz on '${topic}' for you below!")
            } else {
                // Fallback hardcoded quiz if Gemini call fails/throttled
                val fallbackQuiz = Quiz(
                    questions = listOf(
                        QuizQuestion(
                            question = "What is the sutra 'Ekadhikena Purvena' in Vedic Mathematics primarily used for?",
                            options = listOf("Squaring numbers ending with 5", "Multiplying by 11", "Finding square roots", "Submitting algebra equations"),
                            correctAnswerIndex = 0
                        ),
                        QuizQuestion(
                            question = "Which of the following is considered one of the 12 Houses (Bhavas) in Vedic Astrology?",
                            options = listOf("Dharma Bhava", "Lagna Bhava", "Karma Bhava", "All of the above"),
                            correctAnswerIndex = 3
                        ),
                        QuizQuestion(
                            question = "What does 'Samskritam' (Sanskrit) literally mean?",
                            options = listOf("Perfected / Refined", "Ancient tongue", "Sacred script", "Spoken word"),
                            correctAnswerIndex = 0
                        )
                    )
                )
                _activeQuiz.value = fallbackQuiz
                repository.addChatMessage("ai", "Gemini is busy, so I've loaded a curated general knowledge quiz on XtraGyan categories for you!")
            }
            _isAiLoading.value = false
        }
    }

    fun dismissQuiz() {
        _activeQuiz.value = null
        _quizTopic.value = ""
    }

    // Helper to deserialize lessons
    fun getLessonsForCourse(course: CourseEntity): List<Lesson> {
        return try {
            lessonAdapter.fromJson(course.lessonsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // DB Prepopulation logic
    private suspend fun prepopulateDatabase() {
        // Add default sessions
        val currentTime = System.currentTimeMillis()
        val sessions = listOf(
            LiveSessionEntity(
                title = "Live Astrology Q&A: Planetary Retrogrades",
                instructorName = "Acharya Rajesh Sharma",
                category = "Astrology",
                startTime = currentTime + 7200000, // in 2 hours
                durationMinutes = 60,
                meetingLink = "https://meet.google.com/abc-defg-hij",
                description = "Learn how upcoming retrogrades affect your zodiac sign and get live predictions."
            ),
            LiveSessionEntity(
                title = "Vedic Maths Secrets for Competitive Exams",
                instructorName = "Dr. Alok Verma",
                category = "Vedic Mathematics",
                startTime = currentTime + 86400000, // tomorrow
                durationMinutes = 90,
                meetingLink = "https://meet.google.com/xyz-pdqr-lmn",
                description = "Master 5-second calculations to solve quantitative aptitude sections fast!"
            ),
            LiveSessionEntity(
                title = "Introduction to Sanskrit chanting & Pronunciation",
                instructorName = "Dr. Meenakshi Iyer",
                category = "Languages",
                startTime = currentTime + 172800000, // in 2 days
                durationMinutes = 45,
                meetingLink = "https://meet.google.com/mno-stuv-wxy",
                description = "Correct your phonetic articulation of Sanskrit stotras and shlokas for high energy."
            )
        )
        for (s in sessions) repository.insertSession(s)

        // Add default courses
        val astroLessons = listOf(
            Lesson("Understanding the 12 Astrological Houses", "25 mins", "https://example.com/video1", "Learn what each bhava (house) represents in a birth chart, from Lagna to Vyaya."),
            Lesson("Planets and Nakshatras: Cosmic Actors", "30 mins", "https://example.com/video2", "Discover the characteristics of planets (Grahas) and how Nakshatras modify their behavior."),
            Lesson("Planetary Aspects (Drishti) & Conjunctions", "20 mins", "https://example.com/video3", "Master calculations of planetary visions across the houses and their impact."),
            Lesson("Step-by-Step Birth Chart (Kundali) Reading", "45 mins", "https://example.com/video4", "Synthesize your knowledge to read your first complete Kundali step by step.")
        )
        val vedicLessons = listOf(
            Lesson("Ekadhikena Purvena: 5-second Squaring Technique", "15 mins", "https://example.com/video5", "Square any number ending in 5 instantly in your mind with zero scratch work!"),
            Lesson("Nikhilam Navatashcaramam Multiplication Sutra", "20 mins", "https://example.com/video6", "Multiply numbers close to base values (like 10, 100, 1000) inside 3 seconds."),
            Lesson("Calculating Square Roots Mentally", "18 mins", "https://example.com/video7", "Sutra 'Duplex' application to discover square roots of large numbers instantly."),
            Lesson("Urdhva-Tiryagbhyam: General Cross-Multiplication", "22 mins", "https://example.com/video8", "The ultimate multiplication formula for multiplying any dimensions of numbers.")
        )
        val androidLessons = listOf(
            Lesson("Jetpack Compose Layouts and Edge-To-Edge Designing", "40 mins", "https://example.com/video9", "Master building gorgeous screen structures adhering to strict Google Material 3 guidelines."),
            Lesson("M3 Themes, Colors, and Typography Pairing", "30 mins", "https://example.com/video10", "Unify your application visual styles, configure custom color schemes and typography classes."),
            Lesson("Room Database and local SQLite state persistence", "45 mins", "https://example.com/video11", "Implement repository patterns, Flow-based data streaming, and Room DB configurations."),
            Lesson("RESTful Integrations with Gemini API via Retrofit", "50 mins", "https://example.com/video12", "Call modern preview LLMs directly, pass Chat History, parse Structured JSON responses.")
        )
        val ritualLessons = listOf(
            Lesson("Pranayama and Purification Chants", "15 mins", "https://example.com/video13", "Purify your inner environment before starting puja with standard vedic breathing mantras."),
            Lesson("Ganesh Puja Step-by-Step Procedure", "25 mins", "https://example.com/video14", "Learn the primary shodashopachara (16-step) puja of Lord Ganesha with correct vidhis."),
            Lesson("Important Sanskrit Shlokas & Correct Articulation", "20 mins", "https://example.com/video15", "Practice correction of complex mantras like Gayatri Mantra and Mahamrityunjaya Mantra.")
        )

        val coursesList = listOf(
            CourseEntity(
                title = "AstroGyan: Learn Kundali Reading from Scratch",
                description = "Become a subject-matter expert in Vedic Astrology. This course covers everything from understanding the 12 houses to interpreting planetary transit effects.",
                instructorId = "teacher_rajesh",
                instructorName = "Acharya Rajesh Sharma",
                category = "Astrology",
                price = 499.0,
                rating = 4.8f,
                language = "Hindi",
                duration = "8 Hours",
                lessonsJson = lessonAdapter.toJson(astroLessons) ?: "[]",
                enrollmentCount = 120
            ),
            CourseEntity(
                title = "Vedic Maths Mastery: Calculate 10x Faster",
                description = "Unlock the power of ancient Indian mathematics. Learn 16 simple mental sutras to calculate division, multiplication, and roots faster than a calculator!",
                instructorId = "teacher_alok",
                instructorName = "Dr. Alok Verma",
                category = "Vedic Mathematics",
                price = 299.0,
                rating = 4.9f,
                language = "Hindi",
                duration = "6 Hours",
                lessonsJson = lessonAdapter.toJson(vedicLessons) ?: "[]",
                enrollmentCount = 310
            ),
            CourseEntity(
                title = "AI Android Development with Jetpack Compose",
                description = "A practical handbook for building state-of-the-art Android apps. Implement edge-to-edge screens, Room Database, and direct REST integrations with Gemini API.",
                instructorId = "teacher_avanish",
                instructorName = "Avanish Mishra",
                category = "Programming & AI",
                price = 0.0,
                rating = 4.7f,
                language = "English",
                duration = "12 Hours",
                lessonsJson = lessonAdapter.toJson(androidLessons) ?: "[]",
                enrollmentCount = 145
            ),
            CourseEntity(
                title = "Practical Vedic Rituals & Shlokas for Daily Life",
                description = "Incorporate positivity into your household. Master daily prayers, Ganesh Puja, Havan mantras, and correct vocal pronunciation techniques of core Sanskrit shlokas.",
                instructorId = "teacher_suresh",
                instructorName = "Pandit Suresh Shastri",
                category = "Rituals & Puja",
                price = 199.0,
                rating = 4.6f,
                language = "Hindi",
                duration = "4 Hours",
                lessonsJson = lessonAdapter.toJson(ritualLessons) ?: "[]",
                enrollmentCount = 85
            )
        )

        for (c in coursesList) repository.insertCourse(c)
    }
}

class XtraGyanViewModelFactory(
    private val application: Application,
    private val repository: AppRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(XtraGyanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return XtraGyanViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
