# Staging Smoke Test

## 1. Purpose

Validate the MVP path in a staging-like environment after deployment.

This smoke test is intentionally manual and lightweight.

## 2. Preconditions

- backend deployed
- frontend deployed
- Redis reachable
- Qdrant reachable
- OpenAI configured
- Brevo configured
- recruiter-access enabled
- frontend base URL and origin correctly configured
- test recruiter email available

## 3. Smoke Flow

### Step 1: Health

- call `GET /actuator/health`
- expect HTTP 200
- verify readiness/liveness sub-statuses if exposed

### Step 2: Access screen

- open frontend
- expect private-access screen
- verify wording is recruiter-first
- verify email input is present

### Step 3: Invitation request

- submit recruiter email
- expect generic success message
- verify no frontend error

### Step 4: Email delivery

- confirm invitation email is received
- verify sender identity
- verify link target uses the expected frontend base URL
- verify expiry message is present

### Step 5: Invitation consumption

- open invitation link
- expect authenticated access to private portfolio
- verify recruiter session cookie is set

### Step 6: Private homepage

- verify hero, recruiter summary, target roles, and contact CTA are visible
- verify page is useful without chat

### Step 7: Grounded chat

- ask one factual question:
  - `Quel est son poste actuel et ses responsabilites ?`
- expect a short factual answer
- ask one follow-up:
  - `vas-y`
- expect a context-aware follow-up

### Step 8: Invalid-link behavior

- reuse the same invitation link
- expect `already used` behavior without unauthorized private access

### Step 9: Logout

- click logout
- expect return to access screen
- verify private endpoints are no longer accessible from the browser session

## 4. Expected Outcome

The smoke test is successful if:

- access flow works end to end
- email is delivered correctly
- session handling is correct
- homepage is recruiter-first
- chat remains grounded and concise
- logout works

## 5. Failure Classification

### Blocker

- no email delivery
- invalid link handling broken
- no recruiter session after consume
- homepage inaccessible after valid login
- chat unavailable or ungrounded

### Medium risk

- wording or UX confusion
- weak but usable chat answer
- minor contact CTA issue

## 6. Evidence to Capture

- one screenshot of access screen
- one screenshot of private homepage
- one screenshot of chat answer
- one copy of key logs around invite and consume
