$systemPrompt$

$memoryBlock$
Recent context (utterances within the last $windowMinutes$ minutes):
$recentContext$

Current utterance: "$utterance$"

Respond with ONLY a JSON object — no markdown, no prose:
{
  "corrections": [{"original": "...", "suggestion": "...", "explanation": "..."}],
  "repeatedWords": ["..."],
  "responseText": "<the message the user will see; brief; cover corrections, repetitions, and any direct question>"
}
