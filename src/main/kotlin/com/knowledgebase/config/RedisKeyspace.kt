package com.knowledgebase.config

object RedisKeyspace {
    const val SESSION_PREFIX = "session:"
    const val CHAT_MEMORY_PREFIX = "chat:memory:"
    const val INGESTED_SOURCES_KEY = "kb:ingested:sources"
    const val INGESTED_FILES_KEY = "kb:ingested:files"
    const val RECRUITER_INVITATION_PREFIX = "recruiter:invitation:"
    const val RECRUITER_INVITATION_EMAIL_PREFIX = "recruiter:invitation-email:"
    const val RECRUITER_SESSION_PREFIX = "recruiter:session:"
    const val RECRUITER_RATE_LIMIT_IP_PREFIX = "recruiter:rate-limit:ip:"
    const val RECRUITER_RATE_LIMIT_EMAIL_PREFIX = "recruiter:rate-limit:email:"
}
