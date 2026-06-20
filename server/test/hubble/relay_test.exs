defmodule Hubble.RelayTest do
  use ExUnit.Case, async: true

  test "deposited envelopes are returned to the recipient, then drained" do
    now = 1_000
    m = Hubble.Relay.put(%{}, "boxA", "env1", now)
    m = Hubble.Relay.put(m, "boxA", "env2", now)
    {blobs, m2} = Hubble.Relay.take(m, "boxA", now)
    assert blobs == ["env1", "env2"]
    # drained: a second collect is empty
    {blobs2, _} = Hubble.Relay.take(m2, "boxA", now)
    assert blobs2 == []
  end

  test "mailboxes are isolated from each other" do
    m = %{} |> Hubble.Relay.put("boxA", "a", 0) |> Hubble.Relay.put("boxB", "b", 0)
    {blobs, _} = Hubble.Relay.take(m, "boxA", 0)
    assert blobs == ["a"]
  end

  test "expired envelopes are not delivered" do
    m = Hubble.Relay.put(%{}, "boxA", "stale", 0)
    {blobs, _} = Hubble.Relay.take(m, "boxA", 1_000_000_000, ttl_ms: 1000)
    assert blobs == []
  end

  test "expire sweeps stale entries and empties mailboxes" do
    m =
      %{}
      |> Hubble.Relay.put("boxA", "old", 0)
      |> Hubble.Relay.put("boxA", "new", 1_000_000)

    swept = Hubble.Relay.expire(m, 1_000_000, ttl_ms: 900_000)
    assert swept == %{"boxA" => [%{data: "new", at_ms: 1_000_000}]}
  end

  test "the relay stores only opaque bytes (no identity, no plaintext)" do
    # A relay entry is just data + timestamp — nothing identifying.
    m = Hubble.Relay.put(%{}, "opaque-mailbox-id", <<1, 2, 3, 4>>, 0)
    assert m == %{"opaque-mailbox-id" => [%{data: <<1, 2, 3, 4>>, at_ms: 0}]}
  end
end
