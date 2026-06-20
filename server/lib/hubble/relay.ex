defmodule Hubble.Relay do
  @moduledoc """
  Pure store-and-forward relay logic. Holds **opaque encrypted envelopes** for offline
  friends, keyed only by the recipient's unlinkable mailbox id (derived client-side from
  the recipient's public key). The server never sees plaintext, sender, or recipient
  identity — just `mailbox_id => [{blob, at_ms}]`.

  Delivery is drain-on-collect: once a recipient collects their mailbox the blobs are
  removed. Anything older than [@ttl_ms] is swept so nothing lingers.

  These functions are pure; `Hubble.Relay.Server` owns the mutable state.
  """

  @type blob :: %{data: binary(), at_ms: integer()}
  @type mailboxes :: %{optional(String.t()) => [blob]}

  # 7 days: long enough for offline friends, short enough to stay "transient".
  @default_ttl_ms 604_800_000

  @doc "Append an opaque envelope to a recipient's mailbox."
  @spec put(mailboxes, String.t(), binary(), integer()) :: mailboxes
  def put(mailboxes, mailbox_id, data, now_ms) when is_binary(data) do
    entry = %{data: data, at_ms: now_ms}
    Map.update(mailboxes, mailbox_id, [entry], fn existing -> existing ++ [entry] end)
  end

  @doc "Collect (and remove) all non-expired envelopes for a mailbox; returns {blobs, mailboxes}."
  @spec take(mailboxes, String.t(), integer(), keyword()) :: {[binary()], mailboxes}
  def take(mailboxes, mailbox_id, now_ms, opts \\ []) do
    ttl = Keyword.get(opts, :ttl_ms, @default_ttl_ms)
    entries = Map.get(mailboxes, mailbox_id, [])
    fresh = Enum.filter(entries, fn e -> now_ms - e.at_ms < ttl end)
    {Enum.map(fresh, & &1.data), Map.delete(mailboxes, mailbox_id)}
  end

  @doc "Drop expired envelopes across all mailboxes (and any mailbox left empty)."
  @spec expire(mailboxes, integer(), keyword()) :: mailboxes
  def expire(mailboxes, now_ms, opts \\ []) do
    ttl = Keyword.get(opts, :ttl_ms, @default_ttl_ms)

    mailboxes
    |> Enum.map(fn {id, entries} ->
      {id, Enum.filter(entries, fn e -> now_ms - e.at_ms < ttl end)}
    end)
    |> Enum.reject(fn {_id, entries} -> entries == [] end)
    |> Map.new()
  end
end
