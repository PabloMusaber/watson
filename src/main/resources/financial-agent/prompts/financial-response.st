$systemPrompt$

$memoryBlock$
Recent conversation:
$recentConversation$

User query: "$userQuery$"

Use the available broker tools to fetch live data as needed, then respond with ONLY a JSON object — no markdown, no prose:
{
  "spokenResponse": "<concise voice-friendly answer, 1–3 sentences, no raw tables>",
  "terminalOutput": "<formatted text or ASCII table of retrieved data>"
}
