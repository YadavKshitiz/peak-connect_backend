package com.peakconnect.entity

enum class Role { TREKKER, GUIDE, ADMIN }
enum class ExperienceLevel { BEGINNER, INTERMEDIATE, EXPERT }
enum class DifficultyLevel { EASY, MODERATE, CHALLENGING, EXTREME }
enum class Season { LOW, SHOULDER, PEAK }
enum class BookingStatus { PENDING, PENDING_GUIDE_RESPONSE, AWAITING_PAYMENT, CONFIRMED, CANCELLED, NO_SHOW, REMATCH_FAILED }
enum class CancellationPolicyType { FLEXIBLE, MODERATE, STRICT }
enum class WaitlistStatus { WAITING, PROMOTED, EXPIRED, CONFIRMED }
