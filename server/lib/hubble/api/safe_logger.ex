defmodule Hubble.Api.SafeLogger do
  @moduledoc """
  Request logger that redacts opaque route ids before they reach the log.

  The server's privacy invariant is that mailbox ids and signaling peer ids
  must never end up in a log line — even though they are HKDF-derived and
  unlinkable, the *same* id appearing repeatedly from the same client is
  enough to fingerprint a relationship without ever decrypting an envelope.
  `Plug.Logger` writes the raw request path, which would leak those ids, so
  the router uses this plug instead.
  """
  @behaviour Plug
  require Logger

  @impl true
  def init(opts), do: opts

  @impl true
  def call(conn, _opts) do
    start = System.monotonic_time()

    Plug.Conn.register_before_send(conn, fn conn ->
      ms =
        System.monotonic_time()
        |> Kernel.-(start)
        |> System.convert_time_unit(:native, :millisecond)

      Logger.info(fn ->
        "#{conn.method} #{redact(conn.request_path)} -> #{conn.status} (#{ms}ms)"
      end)

      conn
    end)
  end

  @doc """
  Collapse an opaque path segment to `_` so it never reaches the log line.
  """
  def redact("/mailbox/" <> _rest), do: "/mailbox/_"
  def redact("/signal/" <> _rest), do: "/signal/_"
  def redact(path), do: path
end
