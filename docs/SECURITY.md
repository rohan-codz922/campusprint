# CampusPrint: security notes

## What is protected, and how
| Risk | Protection |
|---|---|
| Fake email addresses | Accounts stay locked until the 6-digit emailed code is entered. Unverified accounts cannot log in. |
| Password guessing | BCrypt hashes; 5 wrong passwords lock that account for 15 minutes; 10 login requests per minute per IP. |
| Code guessing | Codes expire in 10 minutes, die after 5 wrong tries, one new code per minute, stored only as a hash. |
| Email bombing / spam | 8 requests per minute per IP on every code-sending endpoint. |
| Stolen / old tokens | Tokens last 12 hours and stop working the moment the password changes. |
| Privilege abuse | Roles enforced on the server for every route. Students only ever see their own jobs, files and screenshots. |
| Lying about page count | The server counts PDF pages itself. Students cannot type a page count. |
| Lying about price | The server calculates the price. |
| Walking away from big jobs | 20+ pages (pages x copies) cannot start until the payment screenshot is verified by the print desk. Enforced by the server. |
| Disguised uploads | File type is read from the file's first bytes. Only PDF, JPG and PNG are accepted, with size limits (10 MB / 3 MB). |
| Hard-coded secrets | None. JWT secret and admin login come only from environment variables; the app will not start without them. The website contains no keys. |
| Browser attacks | Strict Content-Security-Policy and other headers on the website; CORS limited to our own site; API sends restrictive headers. |
| Information leaks | Same replies whether or not an email exists (reset/resend); errors never show internals; shop job numbers are not shown to students. |

## Known limits (be honest about these)
- Rate-limit counters are in memory and reset when the server restarts. The per-account protections do not depend on IP.
- The login token is kept in the browser's local storage. The strict content-security policy reduces the risk of script theft; a later upgrade is cookie-based sessions.
- UPI payments are checked by a person looking at a screenshot. Screenshots can be faked, so the print desk should still match the amount in its own bank or UPI app.
- Emails come from a free-mail sender, so some will land in spam until the shop has its own domain.
- A person with access to the Render or Neon account can read all data. Keep those accounts protected with two-factor login.
