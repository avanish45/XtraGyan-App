package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String, // "Learner", "Teacher", "Admin", "Guest"
    val skills: String = "", // Comma-separated list
    val interests: String = "", // Comma-separated list
    val languages: String = "English, Hindi", // Comma-separated list
    val profilePic: String = "",
    val earnings: Double = 0.0,
    val studentCount: Int = 0
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val instructorId: String,
    val instructorName: String,
    val category: String,
    val price: Double,
    val rating: Float,
    val language: String,
    val duration: String,
    val lessonsJson: String, // JSON array of Lesson objects
    val isPublished: Boolean = true,
    val enrollmentCount: Int = 0
)

@Entity(tableName = "enrollments")
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val courseId: Int,
    val progress: Int = 0, // Percentage 0 to 100
    val completedLessons: String = "", // Comma-separated index of completed lessons (e.g. "0,1")
    val isCompleted: Boolean = false,
    val isBookmarked: Boolean = false,
    val certificateId: String? = null,
    val rating: Float? = null,
    val review: String? = null
)

@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val instructorName: String,
    val category: String,
    val startTime: Long, // timestamp
    val durationMinutes: Int,
    val meetingLink: String,
    val description: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class Lesson(
    val title: String,
    val duration: String,
    val videoUrl: String, // simulated URL
    val description: String
)

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

@JsonClass(generateAdapter = true)
data class Quiz(
    val questions: List<QuizQuestion>
)
