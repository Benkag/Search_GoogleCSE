# Search_GoogleCSE
# 1. Project Title

Internet News Search Application (Java Swing + Google Custom Search API)

# 2. Overview

This project is a desktop application built with Java Swing that allows users to search information on the Internet by entering one or more keywords. The application sends a request to Google Custom Search JSON API and displays the returned results in a friendly user interface.

The UI is designed as a three-column layout:

Left panel (Website list / domains): shows the domains extracted from search results (e.g., dantri.com.vn, vnexpress.net).

Middle panel (Keywords & actions): contains keyword inputs and action buttons.

Right panel (Results): shows titles, snippets, and links of found web pages.

# 3. Objectives

Provide a simple and fast way to search for news or information based on user keywords.

Display results clearly (title, snippet, source domain).

Support a “price search” mode to search product prices more effectively (by appending “giá” to the query and highlighting common price patterns).

# 4. Technologies Used

Language: Java (JDK 17/22+)

User Interface: Java Swing

HTTP Requests: java.net.http.HttpClient (built-in from Java 11)

JSON Parsing: Jackson ObjectMapper

Optional UI Theme: FlatLaf (if enabled)

Search Service: Google Custom Search JSON API using:

API Key

CX (Search Engine ID)

# 5. System Architecture (How it works)
Input → API Request → Output

User enters keywords (Keyword 1 is required, Keyword 2/3 optional).

The application builds a query string:

Normal search: keyword1 keyword2 keyword3

Price search: keyword1 keyword2 keyword3 giá

The application sends HTTP GET request to:
https://www.googleapis.com/customsearch/v1?key=...&cx=...&q=...

Google returns a JSON response containing an array of result items.

The application parses JSON fields such as:

title

link

snippet

displayLink

Results are rendered in the UI:

Domains list (left) is generated from displayLink.

Result list (right) shows title, snippet, and domain.

When a user double-clicks a result, the application opens the URL in the default browser.

# 6. Key Features

Multi-keyword search: Up to 3 input fields.

Website/domain list: Extracted automatically from results.

Domain filtering (optional): Clicking a domain can filter results to that domain.

Price search mode: Adds “giá” to query and highlights price patterns like “1.200.000đ”, “299k”.

Clickable results: Open in browser.

Asynchronous search: Uses SwingWorker to avoid freezing the UI while fetching data.

# 7. Error Handling

The application handles common cases:

Missing API configuration (API Key/CX not set).

API error responses (HTTP 400/403/429).

Network timeout or connectivity problems.
In case of errors, the system shows an error pop-up with the API message.

# 8. Limitations

The Google Custom Search API has daily quota limits (e.g., free requests/day).

Search quality depends on the configuration of the Programmable Search Engine (PSE).

If API key is restricted incorrectly, requests may return HTTP 403.

# 9. Conclusion

This project demonstrates how to build a Java desktop application integrated with a real web search API. The user interface is simple and effective, and the application can be extended with additional features such as pagination, saving history, exporting results, or supporting multiple search engines.

USER GUIDE (How to Use the Program)
# A. Requirements

Java installed (JDK 17+ recommended).

Internet connection.

A valid Google Custom Search API Key and CX (Search Engine ID).

(If used) Dependencies installed via Maven (Jackson / FlatLaf).

# B. API Setup (Important)

Before running the program, you must have:

API Key from Google Cloud Console (Custom Search API enabled)

CX from Programmable Search Engine settings

Recommended: Do NOT hard-code the key

For security, do not paste the key directly inside your source code. Use one of these safe methods:

Set environment variables, or

Use application settings dialog (if your UI version includes it)

# C. Launching the Application in NetBeans

Open the project in Apache NetBeans IDE 27.

Ensure the project compiles without errors.

Run the main class (e.g., GoogleCSE_SearchApp or BaiTapLon launcher class).

If the program shows “Missing configuration”, set API Key/CX properly (see Section D).

# D. Configure API Key and CX
Option 1 — Using Environment Variables (Windows)

Open PowerShell and run:

setx GOOGLE_CSE_KEY "YOUR_KEY"

setx GOOGLE_CSE_CX "YOUR_CX"

Then restart NetBeans and run again.

Option 2 — Using NetBeans Run Configuration

In Project Properties → Configurations/Run, add JVM arguments:

-DGOOGLE_CSE_KEY=YOUR_KEY

-DGOOGLE_CSE_CX=YOUR_CX

(Then update code to read System.getProperty.)

Option 3 — In-app Settings (If enabled)

Click ⚙ Settings and paste API Key and CX, then click Save.

# E. Searching

Enter Keyword 1 (required).

Optionally enter Keyword 2 and Keyword 3.

Click:

Search to perform a normal search, or

Find product price to search for price-related results.

The results will appear on the right panel.

# F. Viewing and Opening Results

Each result shows:

Title

Domain/source

Snippet (short description)

Double-click a result item to open the web page in your default browser.

# G. Filtering by Website (if your version supports it)

Click on a domain in the left list (e.g., dantri.com.vn)

The results list will filter to show only results from that domain.

Click “All” (or “Tất cả”) to reset.

# H. Troubleshooting

Error 403: API key restrictions or API not enabled.

Error 400: Invalid parameters or wrong CX.

Error 429: Quota exceeded.

No results returned: Try different keywords or check PSE configuration.

# Video Demo
https://youtu.be/9pJiu4RAMEI
