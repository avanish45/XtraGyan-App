package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CourseEntity
import com.example.data.local.EnrollmentEntity
import com.example.data.local.Lesson
import com.example.data.local.LiveSessionEntity
import com.example.data.local.QuizQuestion
import com.example.data.local.UserEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtraGyanApp(viewModel: XtraGyanViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val liveSessions by viewModel.liveSessions.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val enrollments by viewModel.userEnrollments.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val activeQuiz by viewModel.activeQuiz.collectAsState()
    val quizTopic by viewModel.quizTopic.collectAsState()

    var currentRoute by remember { mutableStateOf("discover") }
    val context = LocalContext.current

    // Navigation sub-states (null means main view of route is active)
    var selectedCourseForDetail by remember { mutableStateOf<CourseEntity?>(null) }
    var activeCourseForLearning by remember { mutableStateOf<CourseEntity?>(null) }
    var activeLiveSessionSimulation by remember { mutableStateOf<LiveSessionEntity?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.School, contentDescription = "Discover") },
                    label = { Text("Discover") },
                    selected = currentRoute == "discover",
                    onClick = {
                        currentRoute = "discover"
                        selectedCourseForDetail = null
                        activeCourseForLearning = null
                    },
                    modifier = Modifier.testTag("nav_discover")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Book, contentDescription = "Learning") },
                    label = { Text("My Study") },
                    selected = currentRoute == "learn",
                    onClick = {
                        currentRoute = "learn"
                        selectedCourseForDetail = null
                    },
                    modifier = Modifier.testTag("nav_learn")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Chat, contentDescription = "AI Tutor") },
                    label = { Text("AI Tutor") },
                    selected = currentRoute == "assistant",
                    onClick = {
                        currentRoute = "assistant"
                        selectedCourseForDetail = null
                        activeCourseForLearning = null
                    },
                    modifier = Modifier.testTag("nav_assistant")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == "dashboard",
                    onClick = {
                        currentRoute = "dashboard"
                        selectedCourseForDetail = null
                        activeCourseForLearning = null
                    },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = {
                        currentRoute = "profile"
                        selectedCourseForDetail = null
                        activeCourseForLearning = null
                    },
                    modifier = Modifier.testTag("nav_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                "discover" -> {
                    if (selectedCourseForDetail != null) {
                        CourseDetailScreen(
                            course = selectedCourseForDetail!!,
                            enrollment = enrollments.find { it.courseId == selectedCourseForDetail!!.id },
                            onBack = { selectedCourseForDetail = null },
                            onEnroll = {
                                viewModel.enrollInCourse(selectedCourseForDetail!!.id)
                                Toast.makeText(context, "Successfully enrolled in ${selectedCourseForDetail!!.title}!", Toast.LENGTH_SHORT).show()
                            },
                            onStartLearning = {
                                activeCourseForLearning = selectedCourseForDetail
                                selectedCourseForDetail = null
                                currentRoute = "learn"
                            }
                        )
                    } else {
                        DiscoverScreen(
                            courses = courses,
                            currentUser = currentUser,
                            enrollments = enrollments,
                            liveSessions = liveSessions,
                            onCourseClick = { selectedCourseForDetail = it },
                            onNavigateToTab = { currentRoute = it },
                            onJoinLive = { activeLiveSessionSimulation = it },
                            onAskAI = { query ->
                                viewModel.sendMessageToAi(query)
                                currentRoute = "assistant"
                            }
                        )
                    }
                }
                "learn" -> {
                    if (activeCourseForLearning != null) {
                        val matchingEnrollment = enrollments.find { it.courseId == activeCourseForLearning!!.id }
                        LearningHubScreen(
                            course = activeCourseForLearning!!,
                            enrollment = matchingEnrollment,
                            viewModel = viewModel,
                            onBack = { activeCourseForLearning = null },
                            onAskAI = { topic ->
                                viewModel.sendMessageToAi("Hello AI Tutor, I am studying '${activeCourseForLearning!!.title}' and want to understand more about: $topic")
                                currentRoute = "assistant"
                                activeCourseForLearning = null
                            }
                        )
                    } else {
                        MyStudyScreen(
                            courses = courses,
                            enrollments = enrollments,
                            onCourseClick = { activeCourseForLearning = it },
                            onFindCourses = { currentRoute = "discover" }
                        )
                    }
                }
                "assistant" -> {
                    AiAssistantScreen(
                        chatHistory = chatHistory,
                        isAiLoading = isAiLoading,
                        activeQuiz = activeQuiz,
                        quizTopic = quizTopic,
                        onSendMessage = { viewModel.sendMessageToAi(it) },
                        onGenerateQuiz = { viewModel.generateQuiz(it) },
                        onDismissQuiz = { viewModel.dismissQuiz() },
                        onClearChat = { viewModel.clearChat() }
                    )
                }
                "dashboard" -> {
                    if (currentUser?.role == "Teacher") {
                        TeacherDashboardScreen(
                            viewModel = viewModel,
                            courses = courses.filter { it.instructorId == currentUser?.id },
                            liveSessions = liveSessions.filter { it.instructorName == currentUser?.name }
                        )
                    } else {
                        LearnerDashboardScreen(
                            viewModel = viewModel,
                            courses = courses,
                            enrollments = enrollments,
                            liveSessions = liveSessions,
                            onContinueLearning = { activeCourseForLearning = it; currentRoute = "learn" },
                            onJoinLive = { activeLiveSessionSimulation = it }
                        )
                    }
                }
                "profile" -> {
                    ProfileScreen(
                        currentUser = currentUser,
                        viewModel = viewModel
                    )
                }
            }

            // Simulated Live Meeting Overlay
            if (activeLiveSessionSimulation != null) {
                SimulatedLiveClassScreen(
                    session = activeLiveSessionSimulation!!,
                    onLeave = { activeLiveSessionSimulation = null }
                )
            }
        }
    }
}

// ==================== DISCOVER SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    courses: List<CourseEntity>,
    currentUser: UserEntity?,
    enrollments: List<EnrollmentEntity>,
    liveSessions: List<LiveSessionEntity>,
    onCourseClick: (CourseEntity) -> Unit,
    onNavigateToTab: (String) -> Unit,
    onJoinLive: (LiveSessionEntity) -> Unit,
    onAskAI: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Astrology", "Vedic Mathematics", "Programming & AI", "Rituals & Puja", "Languages")

    val filteredCourses = courses.filter { course ->
        val matchesSearch = course.title.contains(searchQuery, ignoreCase = true) ||
                course.description.contains(searchQuery, ignoreCase = true) ||
                course.instructorName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || course.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    val context = LocalContext.current

    // Initials helper
    val userInitials = remember(currentUser) {
        val name = currentUser?.name ?: "Guest"
        name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Top App Bar / Search Block ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search bar stylized with BentoSandBg & BentoSandBorder
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("discover_search_input"),
                    placeholder = { Text("Search expert courses & teachers...", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF49454F)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BentoSandBg,
                        unfocusedContainerColor = BentoSandBg,
                        focusedBorderColor = SaffronPrimary,
                        unfocusedBorderColor = BentoSandBorder,
                        cursorColor = SaffronPrimary
                    ),
                    shape = RoundedCornerShape(26.dp)
                )

                // Initials Avatar Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SaffronPrimary)
                        .clickable { onNavigateToTab("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitials,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 2. Bento Grid Main Content (only shown when not searching/filtering) ---
        if (searchQuery.isEmpty() && selectedCategory == "All") {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: Primary Action Tile - Continue Learning
                    val activeEnrollment = enrollments.firstOrNull { !it.isCompleted }
                    val activeCourse = activeEnrollment?.let { env -> courses.find { it.id == env.courseId } }
                    val tileCourseTitle = activeCourse?.title ?: "Vedic Mathematics"
                    val tileCourseSubtitle = activeCourse?.let { "By ${it.instructorName}" } ?: "Mastering Speed Calculations"
                    val tileProgress = activeEnrollment?.progress?.toFloat()?.div(100f) ?: 0.65f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(BentoPurpleBg)
                            .clickable {
                                if (activeCourse != null) {
                                    onCourseClick(activeCourse)
                                } else {
                                    val fallback = courses.find { it.category == "Vedic Mathematics" } ?: courses.firstOrNull()
                                    if (fallback != null) onCourseClick(fallback)
                                }
                            }
                            .padding(20.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.4f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (activeEnrollment != null) "Active Now" else "Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoPurpleText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Play",
                                    tint = BentoPurpleText,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = tileCourseTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = BentoPurpleText
                            )
                            Text(
                                text = tileCourseSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = BentoPurpleText.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom Bento Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(tileProgress)
                                        .background(BentoPurpleText)
                                )
                            }
                        }
                    }

                    // Row 2: AI Learning Assistant & Live Session Alert side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AI Tutor Tile (Left, span 3/6)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(BentoDarkTileBg)
                                .clickable { onNavigateToTab("assistant") }
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Sparkle",
                                    tint = BentoPurpleBg,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "AI Tutor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Explain 'Pythagoras' simply...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoDarkTileText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Live Session Alert (Right, span 3/6)
                        val activeLive = liveSessions.firstOrNull()
                        val liveTitle = activeLive?.title ?: "Astrology Q&A"
                        val liveSubtitle = activeLive?.let { "Starts soon" } ?: "Starts in 12 mins"

                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(BentoLiveBg)
                                .border(1.dp, Color(0xFFF9DEDC), RoundedCornerShape(28.dp))
                                .clickable {
                                    if (activeLive != null) {
                                        onJoinLive(activeLive)
                                    } else {
                                        Toast.makeText(context, "Opening Live Q&A Room...", Toast.LENGTH_SHORT).show()
                                        val fallbackLive = LiveSessionEntity(
                                            id = 999,
                                            title = "Astrology Q&A",
                                            instructorName = "Pandit Sharma",
                                            category = "Astrology",
                                            startTime = Date().time + 600000,
                                            durationMinutes = 60,
                                            meetingLink = "http://zoom.us/mock",
                                            description = "Live Q&A on Astrological Houses"
                                        )
                                        onJoinLive(fallbackLive)
                                    }
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BentoLiveDot.copy(alpha = pulseAlpha))
                                    )
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BentoLiveText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = liveTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoLiveText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = liveSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BentoLiveText.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Row 3: Categories Tags & Mentorship/1-on-1 Help
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Categories Quick Access (Left, span 4/6)
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(130.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White)
                                .border(1.dp, BentoBorderColor, RoundedCornerShape(28.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "DISCOVER",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF49454F),
                                    letterSpacing = 1.5.sp
                                )
                                // Category Tag capsules
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Astrology", "Languages", "Rituals").forEach { cat ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(BentoPurpleBg.copy(alpha = 0.4f))
                                                .clickable { selectedCategory = cat }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = BentoPurpleText,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Mentorship Tile (Right, span 2/6)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(130.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(BentoSandBg)
                                .clickable {
                                    onAskAI("Hello! I am looking for 1-on-1 mentorship help with Vedic studies or Programming. Can you connect me with an expert or help guide me?")
                                    Toast.makeText(context, "Opening Mentorship Assistant...", Toast.LENGTH_SHORT).show()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Mentorship help",
                                    tint = Color(0xFF49454F),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1-on-1 Help",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF49454F),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Category selector chips (always show when searching or filtering) ---
        if (searchQuery.isNotEmpty() || selectedCategory != "All") {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SaffronPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // --- 4. Section Heading ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == "All") "Popular Expert Courses" else "$selectedCategory Courses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (selectedCategory != "All" || searchQuery.isNotEmpty()) {
                    TextButton(onClick = {
                        selectedCategory = "All"
                        searchQuery = ""
                    }) {
                        Text("Reset Filters", color = SaffronPrimary)
                    }
                }
            }
        }

        // --- 5. Course list ---
        if (filteredCourses.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "No courses",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No courses found matching your query.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(filteredCourses) { course ->
                CourseCard(course = course, onClick = { onCourseClick(course) })
            }
        }
    }
}

@Composable
fun CourseCard(course: CourseEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_card_${course.id}")
            .border(1.dp, BentoBorderColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = course.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = course.rating.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "By ${course.instructorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = course.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "🕒 ${course.duration}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "🌐 ${course.language}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Text(
                    text = if (course.price == 0.0) "FREE" else "₹${course.price.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (course.price == 0.0) NeonCyan else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==================== COURSE DETAIL SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    course: CourseEntity,
    enrollment: EnrollmentEntity?,
    onBack: () -> Unit,
    onEnroll: () -> Unit,
    onStartLearning: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Badge
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = course.category,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = course.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Rating", tint = SaffronPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${course.rating} / 5.0 (Vouched by users)", style = MaterialTheme.typography.bodyMedium)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Instructor Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = course.instructorName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Subject matter expert with years of teaching experience. Authorized guide on XtraGyan platform.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = "About this Course",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = course.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            Divider()

            Text(
                text = "Course Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Duration", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(course.duration, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Language", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(course.language, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Price", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(if (course.price == 0.0) "Free" else "₹${course.price.toInt()}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SaffronPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (enrollment != null) {
                // Already Enrolled
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "You are enrolled in this course!",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    LinearProgressIndicator(
                        progress = { enrollment.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text("Completed: ${enrollment.progress}%")
                    Button(
                        onClick = onStartLearning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_learn_now"),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text("Continue Learning")
                    }
                }
            } else {
                // Enroll Action
                Button(
                    onClick = onEnroll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_enroll_course"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Text("Enroll Now (Get Access)")
                }
            }
        }
    }
}

// ==================== MY STUDY SCREEN ====================
@Composable
fun MyStudyScreen(
    courses: List<CourseEntity>,
    enrollments: List<EnrollmentEntity>,
    onCourseClick: (CourseEntity) -> Unit,
    onFindCourses: () -> Unit
) {
    val enrolledCourses = courses.filter { course ->
        enrollments.any { it.courseId == course.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Study Desk",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Track your learning journey and active course progress",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (enrolledCourses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = "Empty desk",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "You haven't enrolled in any courses yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Button(
                        onClick = onFindCourses,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text("Explore Knowledge Categories")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(enrolledCourses) { course ->
                    val enrollment = enrollments.first { it.courseId == course.id }
                    Card(
                        onClick = { onCourseClick(course) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("study_course_card_${course.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = course.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (enrollment.isCompleted) {
                                    Surface(
                                        color = NeonCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            "COMPLETED",
                                            color = NeonCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "By ${course.instructorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Progress: ${enrollment.progress}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Duration: ${course.duration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { enrollment.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (enrollment.isCompleted) NeonCyan else SaffronPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onCourseClick(course) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = if (enrollment.isCompleted) NeonCyan else SaffronPrimary)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (enrollment.isCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                                        contentDescription = "Action"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (enrollment.isCompleted) "Review Class Materials" else "Resume Course")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== LEARNING HUB SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningHubScreen(
    course: CourseEntity,
    enrollment: EnrollmentEntity?,
    viewModel: XtraGyanViewModel,
    onBack: () -> Unit,
    onAskAI: (String) -> Unit
) {
    val lessons = viewModel.getLessonsForCourse(course)
    var selectedLessonIndex by remember { mutableIntStateOf(0) }
    var activeTab by remember { mutableStateOf("lessons") } // "lessons", "about", "quiz"
    val context = LocalContext.current

    // Video Player State
    var isPlaying by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableFloatStateOf(0f) }

    // Auto update video simulation
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && videoProgress < 1f) {
                kotlinx.coroutines.delay(1000)
                videoProgress += 0.05f
                if (videoProgress >= 1f) {
                    isPlaying = false
                    Toast.makeText(context, "Completed lesson!", Toast.LENGTH_SHORT).show()
                    viewModel.completeLesson(course.id, selectedLessonIndex, lessons.size)
                }
            }
        }
    }

    val currentLesson = lessons.getOrNull(selectedLessonIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(course.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val isBookmarked = !(enrollment?.isBookmarked ?: false)
                        viewModel.toggleBookmark(course.id, isBookmarked)
                        Toast.makeText(context, if (isBookmarked) "Bookmarked course" else "Removed bookmark", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            if (enrollment?.isBookmarked == true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = SaffronPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Simulated Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (currentLesson != null) {
                        Text(
                            text = "[SIMULATED VIDEO PLAYBACK]",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentLesson.title,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Progress Bar at bottom of player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.DarkGray)
                        .align(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(videoProgress)
                            .background(SaffronPrimary)
                    )
                }
            }

            // Tab bar
            TabRow(selectedTabIndex = when (activeTab) {
                "lessons" -> 0
                "quiz" -> 1
                else -> 2
            }) {
                Tab(selected = activeTab == "lessons", onClick = { activeTab = "lessons" }) {
                    Text("Lessons", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == "quiz", onClick = { activeTab = "quiz" }) {
                    Text("AI Quizzes", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = activeTab == "about", onClick = { activeTab = "about" }) {
                    Text("About", modifier = Modifier.padding(12.dp))
                }
            }

            // Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "lessons" -> {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(lessons) { idx, lesson ->
                                val isCompleted = enrollment?.completedLessons?.split(",")?.contains(idx.toString()) == true
                                Card(
                                    onClick = {
                                        selectedLessonIndex = idx
                                        isPlaying = false
                                        videoProgress = if (isCompleted) 1f else 0f
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedLessonIndex == idx)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${idx + 1}. ${lesson.title}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedLessonIndex == idx)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Duration: ${lesson.duration}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                                            contentDescription = "Status",
                                            tint = if (isCompleted) NeonCyan else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "quiz" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Icon(
                                Icons.Default.QuestionAnswer,
                                contentDescription = "AI Quiz",
                                modifier = Modifier.size(64.dp),
                                tint = SaffronPrimary
                            )
                            Text(
                                "AI-Powered Smart Assessments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unlock direct conceptual assessments. XtraGyan AI Tutor will compile a customized live quiz on the details of this course or specific topics you choose.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Button(
                                onClick = { onAskAI("Generate custom quiz on ${course.title}") },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Generate Interactive Quiz on This Course")
                            }
                            Button(
                                onClick = { onAskAI("Explain difficult concepts from ${course.title}") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ask AI Tutor to Explain Topics")
                            }
                        }
                    }
                    "about" -> {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Course Synopsis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(course.description, style = MaterialTheme.typography.bodyLarge)
                            Divider()
                            Text("Primary Learning Target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("This curriculum was structured by subject matter expert ${course.instructorName} to ensure practical mastery and globally recognized certificates upon 100% completion.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ==================== AI ASSISTANT / CHAT SCREEN ====================
@Composable
fun AiAssistantScreen(
    chatHistory: List<com.example.data.local.ChatMessageEntity>,
    isAiLoading: Boolean,
    activeQuiz: com.example.data.local.Quiz?,
    quizTopic: String,
    onSendMessage: (String) -> Unit,
    onGenerateQuiz: (String) -> Unit,
    onDismissQuiz: () -> Unit,
    onClearChat: () -> Unit
) {
    var textQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll chat to bottom
    LaunchedEffect(chatHistory.size, isAiLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "XtraGyan AI Tutor",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Personalized learning companion & smart quiz generator",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onClearChat) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quiz Container if there is an active quiz generated
        if (activeQuiz != null) {
            QuizVisualCard(
                quiz = activeQuiz,
                topic = quizTopic,
                onClose = onDismissQuiz
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick Prompt Suggestions (Only if chat is mostly empty)
        if (chatHistory.isEmpty()) {
            Text(
                "Suggested Topics:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Explain Nikhilam Multiplication technique in Vedic Math") },
                        label = { Text("⚡ Vedic Math Nikhilam") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Explain what Kundali Bhavas (houses) mean in Astrology") },
                        label = { Text("🔮 Kundali houses") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onGenerateQuiz("Sanskrit Grammar Basics") },
                        label = { Text("🎓 Quiz: Sanskrit Grammar") }
                    )
                }
                item {
                    SuggestionChip(
                        onClick = { onSendMessage("Tell me why Sitar is spiritually significant in Indian Music") },
                        label = { Text("🎵 Sitar significance") }
                    )
                }
            }
        }

        // Chat Bubble List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Lightbulb,
                                contentDescription = "Lightbulb",
                                modifier = Modifier.size(56.dp),
                                tint = SaffronSecondary
                            )
                            Text(
                                "Ask me anything about Vedic Sciences, Languages, Skills or request custom quizzes!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(chatHistory) { msg ->
                    val isUser = msg.sender == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isUser) 16.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isUser) "You" else "XtraGyan AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) Color.LightGray else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (isAiLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("AI is thinking...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = textQuery,
                onValueChange = { textQuery = it },
                placeholder = { Text("Ask your AI Tutor...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_input_text"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (textQuery.trim().isNotEmpty()) {
                        onSendMessage(textQuery.trim())
                        textQuery = ""
                    }
                },
                modifier = Modifier
                    .background(SaffronPrimary, RoundedCornerShape(24.dp))
                    .size(48.dp)
                    .testTag("ai_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

// ==================== INTERACTIVE QUIZ CARD COMPONENT ====================
@Composable
fun QuizVisualCard(
    quiz: com.example.data.local.Quiz,
    topic: String,
    onClose: () -> Unit
) {
    var currentQuestionIdx by remember { mutableIntStateOf(0) }
    var selectedAnswerIdx by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var quizCompleted by remember { mutableStateOf(false) }

    val currentQuestion = quiz.questions.getOrNull(currentQuestionIdx)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_quiz_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)),
        border = BorderStroke(2.dp, SaffronPrimary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interactive Quiz: $topic",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!quizCompleted && currentQuestion != null) {
                Text(
                    text = "Question ${currentQuestionIdx + 1} of ${quiz.questions.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentQuestion.question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                currentQuestion.options.forEachIndexed { optIdx, option ->
                    val isSelected = selectedAnswerIdx == optIdx
                    val isCorrect = currentQuestion.correctAnswerIndex == optIdx

                    val btnColor = when {
                        isSubmitted && isCorrect -> Color(0xFF2E7D32) // Green for correct answer
                        isSubmitted && isSelected && !isCorrect -> Color(0xFFC62828) // Red for selected wrong
                        isSelected -> SaffronPrimary
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val textColor = if (isSelected || (isSubmitted && isCorrect)) Color.White else MaterialTheme.colorScheme.onSurface

                    OutlinedButton(
                        onClick = {
                            if (!isSubmitted) {
                                selectedAnswerIdx = optIdx
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = btnColor),
                        border = BorderStroke(1.dp, if (isSelected) SaffronPrimary else Color.Gray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("quiz_option_$optIdx")
                    ) {
                        Text(text = option, color = textColor, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isSubmitted) {
                    Button(
                        onClick = {
                            if (selectedAnswerIdx != null) {
                                isSubmitted = true
                                if (selectedAnswerIdx == currentQuestion.correctAnswerIndex) {
                                    score++
                                }
                            }
                        },
                        enabled = selectedAnswerIdx != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text("Verify Answer")
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentQuestionIdx < quiz.questions.size - 1) {
                                currentQuestionIdx++
                                selectedAnswerIdx = null
                                isSubmitted = false
                            } else {
                                quizCompleted = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text(if (currentQuestionIdx < quiz.questions.size - 1) "Next Question" else "Show My Scorecard")
                    }
                }
            } else {
                // Scorecard
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = "Completed", tint = NeonCyan, modifier = Modifier.size(56.dp))
                    Text(
                        text = "Quiz Finished!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your Score: $score / ${quiz.questions.size}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = SaffronPrimary
                    )
                    Text(
                        text = if (score == quiz.questions.size) "Mastery Achieved! Incredible work!" else "Good effort! Practice makes perfect.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onClose,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss Quiz & Return to Chat")
                    }
                }
            }
        }
    }
}

// ==================== LEARNER DASHBOARD SCREEN ====================
@Composable
fun LearnerDashboardScreen(
    viewModel: XtraGyanViewModel,
    courses: List<CourseEntity>,
    enrollments: List<EnrollmentEntity>,
    liveSessions: List<LiveSessionEntity>,
    onContinueLearning: (CourseEntity) -> Unit,
    onJoinLive: (LiveSessionEntity) -> Unit
) {
    val completedEnrollments = enrollments.filter { it.isCompleted }
    val ongoingCourses = courses.filter { course ->
        enrollments.any { it.courseId == course.id && !it.isCompleted }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Welcome back, Learner!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Explore upcoming live classes and manage your certificates.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Live Classes section
        item {
            Text(
                text = "Upcoming Live Sessions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (liveSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("No live sessions scheduled at the moment.", modifier = Modifier.padding(16.dp), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(liveSessions) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SaffronPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = session.category,
                                    color = SaffronPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = "Live", tint = Color.Red, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LIVE WEBINAR", color = Color.Red, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Conducted by ${session.instructorName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                            Text(
                                "📅 ${sdf.format(Date(session.startTime))}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Button(
                                onClick = { onJoinLive(session) },
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                                modifier = Modifier.testTag("btn_join_live_${session.id}")
                            ) {
                                Text("Join Live Class")
                            }
                        }
                    }
                }
            }
        }

        // Active Courses list
        item {
            Text(
                text = "Continue Studying",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (ongoingCourses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("No ongoing courses. Enroll in courses to start tracking progress here!", modifier = Modifier.padding(16.dp), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(ongoingCourses) { course ->
                val enroll = enrollments.first { it.courseId == course.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "Overall progress: ${enroll.progress}%", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { enroll.progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = SaffronPrimary,
                                trackColor = BentoBorderColor.copy(alpha = 0.3f)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { onContinueLearning(course) },
                            modifier = Modifier.background(SaffronPrimary, RoundedCornerShape(20.dp))
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Play", tint = Color.White)
                        }
                    }
                }
            }
        }

        // Earned Certificates
        item {
            Text(
                text = "My Verified Certificates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (completedEnrollments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BentoBorderColor, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = "Cert", modifier = Modifier.size(36.dp), tint = Color.Gray)
                        Text(
                            "Finish 100% of any enrolled course syllabus to generate a verified smart certificate instantly!",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(completedEnrollments) { enrollment ->
                val course = courses.find { it.id == enrollment.courseId }
                if (course != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonCyan, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = NeonCyan)
                                Text(
                                    text = enrollment.certificateId ?: "CERT-XTRAGYAN",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "CERTIFICATE OF COMPLETION",
                                style = MaterialTheme.typography.labelSmall,
                                color = SaffronPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Successfully verified on XtraGyan Platform. Authorized by instructor ${course.instructorName}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(viewModel.getApplication(), "Certificate Copied & Ready to Share!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share Certificate")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== TEACHER DASHBOARD SCREEN ====================
@Composable
fun TeacherDashboardScreen(
    viewModel: XtraGyanViewModel,
    courses: List<CourseEntity>,
    liveSessions: List<LiveSessionEntity>
) {
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showScheduleSessionDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Instructor Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage your courses, check student analytics, and host webinars",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Stats Block
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SaffronPrimary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TOTAL EARNINGS", style = MaterialTheme.typography.labelSmall, color = SaffronPrimary, fontWeight = FontWeight.Bold)
                        Text("₹15,400", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("+₹1,200 this week", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("STUDENT ENROLLMENTS", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        Text("42 Active", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("96% Completion Rate", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showAddCourseDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_open_add_course_dialog"),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Publish Course")
                }
                Button(
                    onClick = { showScheduleSessionDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_open_schedule_dialog"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = "Schedule")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Host Live Class")
                }
            }
        }

        // Instructor courses
        item {
            Text(
                text = "My Published Courses (${courses.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (courses.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("You have not published any courses yet. Tap 'Publish Course' above to get started!", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            }
        } else {
            items(courses) { course ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Category: ${course.category} | Fee: ₹${course.price.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rating: ⭐ ${course.rating}", style = MaterialTheme.typography.bodySmall)
                            Text("Enrolled: ${course.enrollmentCount} users", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Scheduled sessions
        item {
            Text(
                text = "My Scheduled Live Webinars",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (liveSessions.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("No live classes scheduled by you yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            }
        } else {
            items(liveSessions) { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = s.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                        Text(text = "Time: ${sdf.format(Date(s.startTime))} | Link: ${s.meetingLink}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onSave = { title, desc, cat, price, lang, dur, lessons ->
                viewModel.createCourse(title, desc, cat, price, lang, dur, lessons)
                showAddCourseDialog = false
            }
        )
    }

    // Schedule Session Dialog
    if (showScheduleSessionDialog) {
        ScheduleSessionDialog(
            onDismiss = { showScheduleSessionDialog = false },
            onSave = { title, cat, delayMs, durMin, link, desc ->
                val startTime = System.currentTimeMillis() + delayMs
                viewModel.scheduleLiveSession(title, cat, startTime, durMin, link, desc)
                showScheduleSessionDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(onDismiss: () -> Unit, onSave: (String, String, String, Double, String, String, List<Lesson>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Astrology") }
    var price by remember { mutableStateOf("299") }
    var language by remember { mutableStateOf("Hindi") }
    var duration by remember { mutableStateOf("4 Hours") }

    val categories = listOf("Astrology", "Vedic Mathematics", "Programming & AI", "Rituals & Puja", "Languages")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Publish Expert Course", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    modifier = Modifier.fillMaxWidth().testTag("add_course_title")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("About Course (Details)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Column {
                    Text("Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Fee (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Language") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (e.g. 5 Hours)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && desc.isNotEmpty()) {
                                // Add 2 default lessons
                                val defaultLessons = listOf(
                                    Lesson("Class 1: Primary Overview and Fundamentals", "30 mins", "https://example.com/video1", "Understand primary aspects and concepts."),
                                    Lesson("Class 2: Practical Exercises and Advanced Guide", "45 mins", "https://example.com/video2", "Live walkthrough of applications.")
                                )
                                onSave(title, desc, category, price.toDoubleOrNull() ?: 0.0, language, duration, defaultLessons)
                            }
                        },
                        enabled = title.isNotEmpty() && desc.isNotEmpty()
                    ) {
                        Text("Publish")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSessionDialog(onDismiss: () -> Unit, onSave: (String, String, Long, Int, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Vedic Mathematics") }
    var duration by remember { mutableStateOf("60") }
    var link by remember { mutableStateOf("https://meet.google.com/abc-def-ghi") }
    var desc by remember { mutableStateOf("") }

    val categories = listOf("Astrology", "Vedic Mathematics", "Programming & AI", "Rituals & Puja", "Languages")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Schedule Live Webinar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Webinar Title") },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_title")
                )

                Column {
                    Text("Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("Meeting Link") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Agenda / Brief Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                onSave(title, category, 3600000, duration.toIntOrNull() ?: 60, link, desc)
                            }
                        },
                        enabled = title.isNotEmpty()
                    ) {
                        Text("Schedule")
                    }
                }
            }
        }
    }
}

// ==================== SIMULATED LIVE CLASSROOM OVERLAY ====================
@Composable
fun SimulatedLiveClassScreen(session: LiveSessionEntity, onLeave: () -> Unit) {
    var chatMsg by remember { mutableStateOf("") }
    val simulatedMessages = remember {
        mutableStateListOf(
            "Ananya: Namaste instructor, happy to join!",
            "Kunal: Sir, does Vedic math apply to calculus?",
            "Meera: Sanskrit chanting has helped me focus so much."
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) {} // block touches underneath
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Video Canvas Grid (Instructor & Participants)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .background(Color.DarkGray)
            ) {
                // Large Instructor View
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Instructor", tint = Color.LightGray, modifier = Modifier.size(80.dp))
                    Text(text = session.instructorName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "PRESENTER (LIVE SCREEN SHARING)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Participant list thumbnail
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(100.dp, 70.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("You (Rahul)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Chat Feed and meeting stats
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = session.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(text = "Room ID: meet-${session.id} | Live (12 participants)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Button(
                        onClick = onLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Leave Class")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chat Messages Scroll
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(simulatedMessages) { msg ->
                        Text(text = msg, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Send live simulated chat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = chatMsg,
                        onValueChange = { chatMsg = it },
                        placeholder = { Text("Comment in live class...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (chatMsg.trim().isNotEmpty()) {
                                simulatedMessages.add("You (Rahul): ${chatMsg.trim()}")
                                chatMsg = ""
                            }
                        },
                        modifier = Modifier.background(SaffronPrimary, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ==================== PROFILE SCREEN ====================
@Composable
fun ProfileScreen(
    currentUser: com.example.data.local.UserEntity?,
    viewModel: XtraGyanViewModel
) {
    var isEditing by remember { mutableStateOf(false) }

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var email by remember(currentUser) { mutableStateOf(currentUser?.email ?: "") }
    var interests by remember(currentUser) { mutableStateOf(currentUser?.interests ?: "") }
    var languages by remember(currentUser) { mutableStateOf(currentUser?.languages ?: "") }
    var skills by remember(currentUser) { mutableStateOf(currentUser?.skills ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Profile Desk",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(SaffronPrimary.copy(alpha = 0.2f), RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Avatar",
                modifier = Modifier.size(64.dp),
                tint = SaffronPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Role: ${currentUser?.role ?: "Guest"}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = SaffronPrimary
                    )
                    Button(
                        onClick = {
                            val nextRole = if (currentUser?.role == "Learner") "Teacher" else "Learner"
                            viewModel.switchUserRole(nextRole)
                        },
                        modifier = Modifier.testTag("btn_switch_role"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Switch to ${if (currentUser?.role == "Learner") "Teacher" else "Learner"}")
                    }
                }

                Divider()

                if (!isEditing) {
                    Text("Name: ${currentUser?.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Email: ${currentUser?.email}", style = MaterialTheme.typography.bodyMedium)
                    if (currentUser?.role == "Teacher") {
                        Text("Teaching Skills: ${currentUser.skills}", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Selected Interests: ${currentUser?.interests}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Fluent Languages: ${currentUser?.languages}", style = MaterialTheme.typography.bodyMedium)

                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth().testTag("btn_edit_profile"),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                    ) {
                        Text("Edit Profile Details")
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (currentUser?.role == "Teacher") {
                        OutlinedTextField(
                            value = skills,
                            onValueChange = { skills = it },
                            label = { Text("My Expert Skills (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = interests,
                            onValueChange = { interests = it },
                            label = { Text("My Interests (comma separated)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = languages,
                        onValueChange = { languages = it },
                        label = { Text("Preferred Languages (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.updateProfile(name, email, interests, languages, skills)
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
