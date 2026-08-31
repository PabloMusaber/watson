## Watson

Watson is a multi-agent setup assistant I develop in order to learn, practice and enjoy **[Embabel Framework](https://github.com/embabel/embabel-agent)** as a tool for creating agents, using its framework routing style.

I started to play with a couple of agents with pretty specific goals:
- Watson is the default agent for small talk conversations.
- The Financial agent has access to an **[MCP server I created for my personal broker](https://github.com/PabloMusaber/ppi-mcp)**, so it can get information from my personal portfolio and help me think about and discuss my financial decisions.
- The English Tutor agent helps me with English corrections when I want to practice my speaking skills.
- The Knowledge agent can access my personal Obsidian vault, and can support me in getting information from there.
- The IT news collector can handle my requests for news, searching the web and sending me the information that it got. This agent has access to Brave Search MCP Server.

I thought of this project as a daemon running on my personal computer, so I wanted to talk with this agent through several channels: WhatsApp, Telegram and even with **my voice** when I am away from my computer.

<div style="text-align: center;">
  <img src="src/main/resources/images/diagram.jpg" width="80%" alt="Solution Diagram">
</div>

#### Interesting features:
- It has conversation history.
- Long-term memory: It can save important information about me, such as preferences and clues about my personality.
- I can switch between Gemini and OpenRouter as providers.
- When I speak to it using my voice, it responds using TTS. This is allowed by a Python microservice connected through WebSocket.