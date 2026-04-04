# Functional Specification

## 1. Product Intent

The product is a private recruiter-facing interactive CV.
Its goal is not to sell software as a standalone commercial product.
Its goal is to help a recruiter, manager, or decision-maker understand Mohamed Daoud's profile faster than with a static CV.

The expected value is:

- rapid profile comprehension
- trustworthy and factual exploration of the profile
- clear conversion toward direct contact
- controlled private access

## 2. Product Positioning

The experience must behave like:

- a premium private portfolio
- an interactive CV
- an AI-assisted profile exploration surface

The experience must not behave like:

- a general-purpose chatbot
- a public marketing website
- a SaaS product homepage

## 3. Primary Users

### Recruiter

Needs:

- understand the profile in less than 2 minutes
- identify job fit quickly
- validate experience, stack, and context
- reach out easily if relevant

### Hiring Manager / Tech Leader

Needs:

- verify seniority and technical depth
- evaluate relevance for a backend or lead role
- inspect architecture, production, and delivery signals
- compare profile against a concrete role

### Product Owner of the portfolio

Needs:

- control access to the portfolio
- ensure factual AI answers
- preserve privacy and a premium framing
- operate the system with low maintenance cost

## 4. Core Functional Scope

The MVP+ target scope is:

- private access by recruiter email request and magic link
- session-based access to the private portfolio
- a recruiter-first landing page with structured profile summary
- static profile sections for skills, experience, and role fit
- AI chat based on portfolio documents and curated profile data
- clear contact paths
- operational visibility for access and chat behavior

## 5. Functional Modules

### 5.1 Recruiter Access

The product must:

- require access control in non-dev environments
- allow a recruiter to request access by email
- send a time-limited invitation link by email
- create a temporary authenticated browser session after link consumption
- reject invalid, expired, or already-used invitation links
- allow logout

The product should:

- explain why email is requested
- explain that access is temporary
- avoid exposing internal security mechanics

### 5.2 Recruiter Landing Experience

The landing experience must:

- explain who Mohamed is
- explain what type of profile this is
- surface seniority, domain, stack, and recent responsibilities quickly
- make clear that AI is a supporting tool, not the main product

The landing experience should:

- be understandable in 10 seconds
- offer direct routes to experience, contact, and chat

### 5.3 Structured Profile Content

The product must expose:

- a short recruiter summary
- key positioning elements
- target roles
- "what this profile can take on quickly"
- skills and architecture topics
- major experiences

Each experience should expose:

- role
- company
- period
- short context
- technical focus
- scope/responsibility
- recruiter-relevant takeaway

### 5.4 AI-Assisted CV Exploration

The AI assistant must:

- answer only on the basis of the profile and indexed documents
- produce short, factual, recruiter-useful answers
- support follow-up questions in a conversation
- refuse or redirect vague questions without context

The AI assistant should:

- guide the recruiter with suggested questions
- explain its scope implicitly through UX and wording
- keep output concise

The AI assistant must not:

- hallucinate skills or experiences
- answer as a general-purpose assistant
- dominate the page over the structured profile

### 5.5 Contact Conversion

The product must:

- provide direct contact paths by email and LinkedIn
- place contact calls to action at natural moments
- keep contact access visible without being aggressive

## 6. Key User Journeys

### Journey A: First-time recruiter access

1. Recruiter opens the site
2. Recruiter sees a private-access screen with clear benefit framing
3. Recruiter submits professional email
4. Recruiter receives a temporary access link
5. Recruiter opens the link
6. Recruiter lands in the private portfolio

Success criteria:

- the recruiter understands the value before submitting email
- access flow feels controlled, not cumbersome

### Journey B: Fast profile evaluation

1. Recruiter lands on the private homepage
2. Recruiter reads the summary and target-role framing
3. Recruiter scans proof blocks and experience cards
4. Recruiter decides whether the profile is relevant

Success criteria:

- role fit can be assessed quickly
- the page is useful even without using chat

### Journey C: Targeted profile verification with AI

1. Recruiter asks a specific question
2. AI answers with concise factual output
3. Recruiter asks a follow-up question
4. Recruiter gets a usable clarification

Success criteria:

- answers are short and grounded
- follow-up messages like "ok", "vas-y", "resume" work if there is context

### Journey D: Contact decision

1. Recruiter decides the profile may be relevant
2. Recruiter uses email or LinkedIn CTA
3. Recruiter initiates a direct exchange

Success criteria:

- contact does not require hunting through the page
- the transition from reading to contacting feels natural

## 7. Functional Rules

### Access Rules

- Access must be private outside `dev` and `test`
- Invitation links must expire
- Browser access session must expire
- Invalid links must never grant access

### Chat Rules

- Empty messages are rejected
- Extremely vague first-turn prompts are short-circuited
- Short follow-up prompts are allowed when conversation context is valid
- Chat answers must remain concise

### Content Rules

- Static profile content is the primary source of orientation
- AI answers must align with profile content
- Contact options must always remain available somewhere visible on the page

## 8. Out of Scope for Current Product

- public anonymous browsing
- multi-user administration portal
- recruiter CRM
- job matching automation
- candidate recommendation engine
- public marketing/blog content

## 9. Functional Acceptance Criteria

The product is functionally acceptable when:

- a recruiter can request access and enter the private space
- the private homepage is useful without chat
- the chat provides factual short answers tied to the profile
- the experiences are recruiter-scannable
- the contact path is clear
- invalid access flows remain safe and understandable
