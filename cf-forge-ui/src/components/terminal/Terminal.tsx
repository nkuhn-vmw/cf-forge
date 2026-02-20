import { useEffect, useRef } from 'react'
import { Terminal as XTerminal } from '@xterm/xterm'
import { FitAddon } from '@xterm/addon-fit'
import { WebLinksAddon } from '@xterm/addon-web-links'
import '@xterm/xterm/css/xterm.css'

export function Terminal({ projectId }: { projectId: string }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const terminalRef = useRef<XTerminal | null>(null)

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
    term.writeln('')
    term.write('\x1b[32m$ \x1b[0m')

    terminalRef.current = term

    const handleResize = () => fitAddon.fit()
    const resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(containerRef.current)

    return () => {
      resizeObserver.disconnect()
      term.dispose()
    }
  }, [projectId])

  return (
    <div
      ref={containerRef}
      className="terminal-container"
    />
  )
}
