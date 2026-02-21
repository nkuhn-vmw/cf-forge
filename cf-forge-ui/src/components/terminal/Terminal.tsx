import { useEffect, useRef, useCallback } from 'react'
import { Terminal as XTerminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { WebLinksAddon } from '@xterm/addon-web-links'
import { api } from '../../api/client.ts'
import '@xterm/xterm/css/xterm.css'

const PROMPT = '\x1b[32m$ \x1b[0m'

export function Terminal({ projectId }: { projectId: string }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const terminalRef = useRef<XTerminal | null>(null)
  const lineBuffer = useRef('')
  const historyRef = useRef<string[]>([])
  const historyIndex = useRef(-1)
  const cursorPos = useRef(0)

  const executeCommand = useCallback(async (cmd: string, term: XTerminal) => {
    const parts = cmd.trim().split(/\s+/)
    const command = parts[0]?.toLowerCase()

    switch (command) {
      case '':
        break

      case 'help':
        term.writeln('\x1b[1;34mCF Forge Terminal — Available Commands:\x1b[0m')
        term.writeln('')
        term.writeln('  \x1b[33mforge build\x1b[0m          Trigger a build for this project')
        term.writeln('  \x1b[33mforge deploy\x1b[0m         Deploy to staging')
        term.writeln('  \x1b[33mforge deploy --prod\x1b[0m  Deploy to production')
        term.writeln('  \x1b[33mforge status\x1b[0m         Show project info')
        term.writeln('  \x1b[33mforge files\x1b[0m          List project files')
        term.writeln('  \x1b[33mclear\x1b[0m                Clear terminal')
        term.writeln('  \x1b[33mhelp\x1b[0m                 Show this help')
        term.writeln('')
        break

      case 'clear':
        term.clear()
        break

      case 'forge':
        await handleForgeCommand(parts.slice(1), term)
        break

      case 'ls':
        await handleLs(parts.slice(1), term)
        break

      case 'cat':
        await handleCat(parts.slice(1), term)
        break

      default:
        term.writeln(`\x1b[31mCommand not found: ${command}\x1b[0m`)
        term.writeln('Type \x1b[33mhelp\x1b[0m for available commands.')
        break
    }
  }, [projectId])

  const handleForgeCommand = useCallback(async (args: string[], term: XTerminal) => {
    const sub = args[0]?.toLowerCase()

    switch (sub) {
      case 'build': {
        term.writeln('\x1b[36mTriggering build...\x1b[0m')
        try {
          const build = await api.builds.trigger(projectId)
          term.writeln(`\x1b[32mBuild started!\x1b[0m ID: ${build.id}`)
          term.writeln(`Status: \x1b[33m${build.status}\x1b[0m`)
        } catch (e) {
          term.writeln(`\x1b[31mBuild failed: ${e instanceof Error ? e.message : 'Unknown error'}\x1b[0m`)
        }
        break
      }

      case 'deploy': {
        const env = args.includes('--prod') ? 'PRODUCTION' : 'STAGING'
        term.writeln(`\x1b[36mDeploying to ${env}...\x1b[0m`)
        try {
          const deploy = await api.deployments.trigger(projectId, env)
          term.writeln(`\x1b[32mDeployment started!\x1b[0m ID: ${deploy.id}`)
          term.writeln(`Strategy: ${deploy.strategy} | Environment: ${deploy.environment}`)
          if (deploy.deploymentUrl) {
            term.writeln(`URL: \x1b[4;36m${deploy.deploymentUrl}\x1b[0m`)
          }
        } catch (e) {
          term.writeln(`\x1b[31mDeploy failed: ${e instanceof Error ? e.message : 'Unknown error'}\x1b[0m`)
        }
        break
      }

      case 'status': {
        term.writeln('\x1b[36mFetching project info...\x1b[0m')
        try {
          const project = await api.projects.get(projectId)
          term.writeln(`\x1b[1mProject:\x1b[0m ${project.name}`)
          term.writeln(`\x1b[1mLanguage:\x1b[0m ${project.language}`)
          term.writeln(`\x1b[1mFramework:\x1b[0m ${project.framework || 'N/A'}`)
          term.writeln(`\x1b[1mBuildpack:\x1b[0m ${project.buildpack || 'auto-detect'}`)
          term.writeln(`\x1b[1mStatus:\x1b[0m ${project.status}`)
        } catch (e) {
          term.writeln(`\x1b[31mFailed: ${e instanceof Error ? e.message : 'Unknown error'}\x1b[0m`)
        }
        break
      }

      case 'files': {
        await handleLs([], term)
        break
      }

      default:
        term.writeln(`\x1b[31mUnknown forge command: ${sub ?? '(none)'}\x1b[0m`)
        term.writeln('Try: \x1b[33mforge build\x1b[0m, \x1b[33mforge deploy\x1b[0m, \x1b[33mforge status\x1b[0m, \x1b[33mforge files\x1b[0m')
        break
    }
  }, [projectId])

  const handleLs = useCallback(async (args: string[], term: XTerminal) => {
    try {
      const dir = args[0] || undefined
      const files = await api.files.list(projectId, dir)
      if (!files.length) {
        term.writeln('\x1b[90m(empty directory)\x1b[0m')
        return
      }
      for (const f of files) {
        if (f.directory) {
          term.writeln(`\x1b[1;34m${f.name}/\x1b[0m`)
        } else {
          term.writeln(`  ${f.name}`)
        }
      }
    } catch (e) {
      term.writeln(`\x1b[31mFailed to list files: ${e instanceof Error ? e.message : 'Unknown error'}\x1b[0m`)
    }
  }, [projectId])

  const handleCat = useCallback(async (args: string[], term: XTerminal) => {
    const path = args[0]
    if (!path) {
      term.writeln('\x1b[31mUsage: cat <filename>\x1b[0m')
      return
    }
    try {
      const result = await api.files.read(projectId, path)
      const lines = result.content.split('\n')
      for (const line of lines) {
        term.writeln(line)
      }
    } catch (e) {
      term.writeln(`\x1b[31mFailed to read file: ${e instanceof Error ? e.message : 'Unknown error'}\x1b[0m`)
    }
  }, [projectId])

  useEffect(() => {
    if (!containerRef.current) return

    const term = new XTerminal({
      theme: {
        background: '#0d1117',
        foreground: '#e6edf3',
        cursor: '#58a6ff',
        selectionBackground: '#264f78',
        black: '#484f58',
        red: '#f85149',
        green: '#3fb950',
        yellow: '#d29922',
        blue: '#58a6ff',
        magenta: '#bc8cff',
        cyan: '#76e3ea',
        white: '#e6edf3',
      },
      fontFamily: "'JetBrains Mono', 'Fira Code', Consolas, monospace",
      fontSize: 13,
      cursorBlink: true,
      convertEol: true,
    })

    const fitAddon = new FitAddon()
    term.loadAddon(fitAddon)
    term.loadAddon(new WebLinksAddon())

    term.open(containerRef.current)
    fitAddon.fit()

    term.writeln('\x1b[1;34mCF Forge Terminal\x1b[0m')
    term.writeln(`\x1b[90mProject: ${projectId}\x1b[0m`)
    term.writeln('Type \x1b[33mhelp\x1b[0m for available commands.')
    term.writeln('')
    term.write(PROMPT)

    terminalRef.current = term
    lineBuffer.current = ''
    cursorPos.current = 0
    historyIndex.current = -1

    // Handle keyboard input
    term.onData((data) => {
      const code = data.charCodeAt(0)

      if (data === '\r') {
        // Enter key
        term.writeln('')
        const cmd = lineBuffer.current
        if (cmd.trim()) {
          historyRef.current.unshift(cmd)
          if (historyRef.current.length > 100) historyRef.current.pop()
        }
        historyIndex.current = -1
        lineBuffer.current = ''
        cursorPos.current = 0
        executeCommand(cmd, term).then(() => {
          term.write(PROMPT)
        })
      } else if (code === 127 || data === '\b') {
        // Backspace
        if (cursorPos.current > 0) {
          const before = lineBuffer.current.slice(0, cursorPos.current - 1)
          const after = lineBuffer.current.slice(cursorPos.current)
          lineBuffer.current = before + after
          cursorPos.current--
          // Move cursor back, rewrite line, clear rest, reposition
          term.write('\b')
          term.write(after + ' ')
          for (let i = 0; i <= after.length; i++) term.write('\b')
        }
      } else if (data === '\x1b[A') {
        // Up arrow — history
        if (historyIndex.current < historyRef.current.length - 1) {
          historyIndex.current++
          replaceLine(term, historyRef.current[historyIndex.current])
        }
      } else if (data === '\x1b[B') {
        // Down arrow — history
        if (historyIndex.current > 0) {
          historyIndex.current--
          replaceLine(term, historyRef.current[historyIndex.current])
        } else if (historyIndex.current === 0) {
          historyIndex.current = -1
          replaceLine(term, '')
        }
      } else if (data === '\x1b[C') {
        // Right arrow
        if (cursorPos.current < lineBuffer.current.length) {
          cursorPos.current++
          term.write(data)
        }
      } else if (data === '\x1b[D') {
        // Left arrow
        if (cursorPos.current > 0) {
          cursorPos.current--
          term.write(data)
        }
      } else if (data === '\x03') {
        // Ctrl+C
        term.writeln('^C')
        lineBuffer.current = ''
        cursorPos.current = 0
        historyIndex.current = -1
        term.write(PROMPT)
      } else if (data === '\x0c') {
        // Ctrl+L — clear
        term.clear()
        term.write(PROMPT)
        lineBuffer.current = ''
        cursorPos.current = 0
      } else if (code >= 32) {
        // Printable character
        const before = lineBuffer.current.slice(0, cursorPos.current)
        const after = lineBuffer.current.slice(cursorPos.current)
        lineBuffer.current = before + data + after
        cursorPos.current += data.length
        term.write(data + after)
        for (let i = 0; i < after.length; i++) term.write('\b')
      }
    })

    function replaceLine(t: XTerminal, newLine: string) {
      // Clear current line content
      const moveBack = cursorPos.current
      for (let i = 0; i < moveBack; i++) t.write('\b')
      t.write(' '.repeat(lineBuffer.current.length))
      for (let i = 0; i < lineBuffer.current.length; i++) t.write('\b')
      // Write new content
      t.write(newLine)
      lineBuffer.current = newLine
      cursorPos.current = newLine.length
    }

    const handleResize = () => fitAddon.fit()
    const resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(containerRef.current)

    return () => {
      resizeObserver.disconnect()
      term.dispose()
    }
  }, [projectId, executeCommand, handleForgeCommand, handleLs, handleCat])

  return (
    <div
      ref={containerRef}
      className="terminal-container"
    />
  )
}
