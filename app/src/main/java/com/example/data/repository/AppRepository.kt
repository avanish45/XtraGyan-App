package com.example.data.repository

import com.example.data.local.*
import com.example.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    // --- Users ---
    fun getUserById(id: String): Flow<UserEntity?> = appDao.getUserById(id)

    suspend fun insertUser(user: UserEntity) = appDao.insertUser(user)

    // --- Courses ---
    val allCourses: Flow<List<CourseEntity>> = appDao.getAllCourses()

    fun getCourseById(id: Int): Flow<CourseEntity?> = appDao.getCourseById(id)

    suspend fun insertCourse(course: CourseEntity): Long = appDao.insertCourse(course)

    suspend fun updateCourse(course: CourseEntity) = appDao.updateCourse(course)

    suspend fun deleteCourse(course: CourseEntity) = appDao.deleteCourse(course)

    // --- Enrollments ---
    fun getUserEnrollments(userId: String): Flow<List<EnrollmentEntity>> = appDao.getUserEnrollments(userId)

    fun getEnrollment(userId: String, courseId: Int): Flow<EnrollmentEntity?> = appDao.getEnrollment(userId, courseId)

    suspend fun enrollInCourse(userId: String, courseId: Int) {
        val existing = appDao.getEnrollmentSuspend(userId, courseId)
        if (existing == null) {
            val enrollment = EnrollmentEntity(
                userId = userId,
                courseId = courseId,
                progress = 0,
                completedLessons = ""
            )
            appDao.insertEnrollment(enrollment)

            // Update course enrollment count
            val course = appDao.getCourseByIdSuspend(courseId)
            if (course != null) {
                appDao.updateCourse(course.copy(enrollmentCount = course.enrollmentCount + 1))
            }
        }
    }

    suspend fun updateEnrollmentProgress(userId: String, courseId: Int, lessonIndex: Int, totalLessons: Int) {
        val existing = appDao.getEnrollmentSuspend(userId, courseId)
        if (existing != null) {
            val completedList = existing.completedLessons.split(",")
                .filter { it.isNotEmpty() }
                .toMutableSet()
            
            completedList.add(lessonIndex.toString())
            val completedStr = completedList.joinToString(",")
            val progress = ((completedList.size.toFloat() / totalLessons) * 100).toInt()
            
            val isCompleted = progress >= 100
            val certificateId = if (isCompleted && existing.certificateId == null) {
                "CERT-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
            } else {
                existing.certificateId
            }

            appDao.updateEnrollment(
                existing.copy(
                    progress = progress,
                    completedLessons = completedStr,
                    isCompleted = isCompleted,
                    certificateId = certificateId
                )
            )
        }
    }

    suspend fun bookmarkCourse(userId: String, courseId: Int, isBookmarked: Boolean) {
        val existing = appDao.getEnrollmentSuspend(userId, courseId)
        if (existing != null) {
            appDao.updateEnrollment(existing.copy(isBookmarked = isBookmarked))
        } else {
            appDao.insertEnrollment(
                EnrollmentEntity(
                    userId = userId,
                    courseId = courseId,
                    isBookmarked = isBookmarked
                )
            )
        }
    }

    suspend fun submitCourseReview(userId: String, courseId: Int, rating: Float, review: String) {
        val existing = appDao.getEnrollmentSuspend(userId, courseId)
        if (existing != null) {
            appDao.updateEnrollment(existing.copy(rating = rating, review = review))
        }
    }

    // --- Live Sessions ---
    val allSessions: Flow<List<LiveSessionEntity>> = appDao.getAllSessions()

    suspend fun insertSession(session: LiveSessionEntity) = appDao.insertSession(session)

    // --- AI Chat ---
    val allChatMessages: Flow<List<ChatMessageEntity>> = appDao.getAllChatMessages()

    suspend fun addChatMessage(sender: String, text: String) {
        appDao.insertChatMessage(ChatMessageEntity(sender = sender, text = text))
    }

    suspend fun clearChatHistory() = appDao.clearChatMessages()

    // --- Gemini Call ---
    suspend fun askGemini(
        prompt: String,
        chatHistory: List<ChatMessageEntity> = emptyList(),
        systemInstruction: String? = null,
        responseMimeType: String? = null
    ): String {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val contents = mutableListOf<GeminiContent>()
        for (msg in chatHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            contents.add(GeminiContent(parts = listOf(GeminiPart(text = msg.text)), role = role))
        }
        contents.add(GeminiContent(parts = listOf(GeminiPart(text = prompt)), role = "user"))

        val req = GeminiRequest(
            contents = contents,
            generationConfig = if (responseMimeType != null) GeminiGenerationConfig(responseMimeType = responseMimeType, temperature = 0.7f) else null,
            systemInstruction = if (systemInstruction != null) GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))) else null
        )

        return try {
            val res = GeminiClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = req
            )
            res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini API"
        } catch (e: Exception) {
            "API Call failed: ${e.localizedMessage ?: e.message}"
        }
    }
}
