# Introduction
Welcome!

RaidBot is a Java based Discord bot that verifies purchases for premium resources on SpigotMC!

# Requirements:
- IntelliJ - *If you wish to edit the code.*
- PayPal Business account - *To use their API so we can verify transactions.*
- Bot token - *So we can run this bad boy.*
- Discord server - *To run the bot in.*
- SpigotMC premium resource - *We have to verify something, right?*

# How to run
To make the bot run:
### Using an executable:
 - Create a new folder and name it whatever you want.
 - Get the compiled version of the bot and add it into the folder. `(RaidBot-1.0.jar)`
 - Make a `.bat` file, name it whatever you like.
 - Add the following to it:
`
@echo off
cd <Folder path here>
java -jar RaidBot-1.0.jar
pause
`
 - Make sure to edit the `<Folder path here>` with the actual folder path.
 - Run the `.bat` and watch the magic happen.

### Using intelliJ:
 - Download the `RaidBot` file.
 - Add it into `C:\Users\<You>\IdeaProjects\` (or the path where your projects are saved)
 - Open IntelliJ and then open the project.
 - Edit at will, then run it within or compile it.

# How to add config data.
First, run the bot one time, this will make it generate a `config.json`, inside there add all the data from the `requirements` above.

# Hosting
You have 2 options:
### Self hosting
You can run the bot locally on your machine, this makes you to:
- Keep your machine up and running 24/7 *(bad)*
- Use your own resources *(also bad, even tho it does not consume a lot)*
### Online hosting
Use a hosting service to run the bot for you. The pro/contra depend on the hosting you choose, overall hosting online can provide:
- 24/7 upkeep
- Graph related to RAM/CPU usage
- File system & backups

# Reccomended hosting
For free hosting as well as paid, the best option I have found is **Pella** - https://www.pella.app

Upsides:
- Free plan
- Supports running of Java bots (something most discord host providers do not have, also the very thing this project is)
- Excellent prices for Paid plans
- Simple, easy to use interface

# Got any issues?
Join my discord over at https://discord.gg/MYK3zTSJkY and let me know! I can help with anything related to: setting it up, hosting question or anything code-wise :)
