import { describe, it, expect, vi } from 'vitest'
import { parseSSEStream } from './client'

/** Create a mock ReadableStreamDefaultReader from string chunks */
function mockReader(chunks: string[]): ReadableStreamDefaultReader<Uint8Array> {
  const encoder = new TextEncoder()
  let index = 0
  return {
    read: vi.fn(async () => {
      if (index >= chunks.length) {
        return { done: true as const, value: undefined }
      }
      const value = encoder.encode(chunks[index++])
      return { done: false as const, value }
    }),
    releaseLock: vi.fn(),
    cancel: vi.fn(async () => {}),
    closed: Promise.resolve(undefined),
  }
}

describe('parseSSEStream', () => {
  it('parses simple data lines', async () => {
    const chunks: string[] = []
    const reader = mockReader(['data: hello\n\n', 'data: world\n\n'])
    await parseSSEStream(reader, (data) => chunks.push(data), () => {})
    expect(chunks).toEqual(['hello', 'world'])
  })

  it('handles partial chunks that split across reads', async () => {
    const chunks: string[] = []
    // "data: partial" split across two reads
    const reader = mockReader(['data: par', 'tial\n\n'])
    await parseSSEStream(reader, (data) => chunks.push(data), () => {})
    expect(chunks).toEqual(['partial'])
  })

  it('ignores [DONE] sentinel', async () => {
    const chunks: string[] = []
    const reader = mockReader(['data: hello\n\ndata: [DONE]\n\n'])
    await parseSSEStream(reader, (data) => chunks.push(data), () => {})
    expect(chunks).toEqual(['hello'])
  })

  it('ignores comment lines and non-data fields', async () => {
    const chunks: string[] = []
    const reader = mockReader([': this is a comment\nevent: message\ndata: payload\n\n'])
    await parseSSEStream(reader, (data) => chunks.push(data), () => {})
    expect(chunks).toEqual(['payload'])
  })

  it('calls onDone when stream ends', async () => {
    const onDone = vi.fn()
    const reader = mockReader(['data: x\n\n'])
    await parseSSEStream(reader, () => {}, onDone)
    expect(onDone).toHaveBeenCalledOnce()
  })

  it('releases the reader lock when done', async () => {
    const reader = mockReader(['data: x\n\n'])
    await parseSSEStream(reader, () => {}, () => {})
    expect(reader.releaseLock).toHaveBeenCalledOnce()
  })

  it('handles empty stream', async () => {
    const chunks: string[] = []
    const onDone = vi.fn()
    const reader = mockReader([])
    await parseSSEStream(reader, (data) => chunks.push(data), onDone)
    expect(chunks).toEqual([])
    expect(onDone).toHaveBeenCalledOnce()
  })

  it('flushes remaining buffer on stream end', async () => {
    const chunks: string[] = []
    // No trailing newline — data is in the buffer when done=true
    const reader = mockReader(['data: tail'])
    await parseSSEStream(reader, (data) => chunks.push(data), () => {})
    expect(chunks).toEqual(['tail'])
  })

  it('aborts when signal is aborted', async () => {
    const controller = new AbortController()
    controller.abort()
    const chunks: string[] = []
    const onDone = vi.fn()
    const reader = mockReader(['data: should-not-appear\n\n'])
    await parseSSEStream(reader, (data) => chunks.push(data), onDone, controller.signal)
    expect(chunks).toEqual([])
    expect(onDone).not.toHaveBeenCalled()
    expect(reader.cancel).toHaveBeenCalled()
  })

  it('handles multiple data lines in one chunk', async () => {
    const chunks: string[] = []
    const reader = mockReader(['data: one\ndata: two\ndata: three\n'])
    await parseSSEStream(reader, (data) => chunks.push(data), () => {})
    expect(chunks).toEqual(['one', 'two', 'three'])
  })
})
