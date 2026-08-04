# Tetris (Java)

A classic Tetris game built with plain Java (Swing) — no external libraries required.

## Controls
| Key | Action |
|---|---|
| ← / → | Move left / right |
| ↑ | Rotate |
| ↓ | Soft drop |
| Space | Hard drop |
| P | Pause / Resume |
| R | Restart (also works after Game Over) |

## Requirements
- JDK 8 or newer (JDK 17/21 recommended). Check with:
  ```
  java -version
  javac -version
  ```
  If these commands aren't found, install a JDK (e.g. [Adoptium Temurin](https://adoptium.net/)).

## How to Compile & Run

### Option 1: Command line
```bash
cd src
javac Tetris.java
java Tetris
```

### Option 2: VS Code
1. Install the **"Extension Pack for Java"** (by Microsoft) from the VS Code Extensions marketplace.
2. Open this folder (`tetris-java`) in VS Code: `File > Open Folder`.
3. Open `src/Tetris.java`.
4. Click the **Run** button above the `main` method (or press `F5` / use the "Run Java" code lens).
   - VS Code will compile and run it automatically — no manual `javac` needed.

## Project Structure
```
tetris-java/
├── src/
│   └── Tetris.java   # entire game (window, board, game loop, rendering)
├── .gitignore
└── README.md
```

## Pushing to GitHub

From inside the `tetris-java` folder:
```bash
git init
git add .
git commit -m "Initial commit: Java Tetris game"
git branch -M main
git remote add origin https://github.com/<your-username>/<repo-name>.git
git push -u origin main
```

To create the empty repo first, either:
- Go to github.com → **New repository** → don't initialize with a README (since you already have one), then copy the URL it gives you into the `git remote add origin` command above, **or**
- Use GitHub CLI: `gh repo create tetris-java --public --source=. --remote=origin --push`

## Possible Improvements
- Ghost piece (preview of where the piece will land)
- Hold-piece feature
- High score saved to a local file
- Sound effects
