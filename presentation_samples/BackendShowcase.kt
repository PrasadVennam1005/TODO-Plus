package presentation_samples

/**
 * Welcome to the TODO++ Showcase!
 * 
 * This file demonstrates the powerful new tagging features available via TODO++ comments, 
 * beautifully rendered in your tool window.
 */
class BackendShowcase {
    
    // TODO(@david priority:CRITICAL category:security due:2023-01-01 issue:PROJ-101): Fix the SQL injection vulnerability here (OVERDUE!)
    fun loadUserProfile(userId: String) {
        // ...
        
        // TODO(priority:MEDIUM category:optimization risk:low): Cache this query response
        val profile = "SELECT * FROM users WHERE id = $userId"
    }

    // TODO(@sarah priority:HIGH due:2028-12-31 category:feature): Add OAuth 2.0 flow for Google Login
    fun authenticate() {
        // FIXME: This token expires too quickly (Basic FIXME still works!)
        val token = "xyz123"
    }
}
