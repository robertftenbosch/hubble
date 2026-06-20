defmodule Hubble.SignalingTest do
  use ExUnit.Case, async: false

  # The Hubble.Signaling.Registry is started by Hubble.Application for the test run.

  test "a message is routed only to the addressed peer" do
    # This process plays peer "bob"; register and expect delivery.
    Hubble.Signaling.register("bob")

    Hubble.Signaling.deliver("bob", %{"from" => "alice", "type" => "offer", "payload" => "sdp"})
    assert_receive {:signal, %{"from" => "alice", "type" => "offer", "payload" => "sdp"}}, 500

    # A message to someone else does not arrive here.
    Hubble.Signaling.deliver("carol", %{"from" => "alice", "type" => "ice"})
    refute_receive {:signal, _}, 200
  end

  test "delivering to an unknown peer is a no-op (no crash)" do
    assert Hubble.Signaling.deliver("nobody-here", %{"type" => "answer"}) == :ok
  end
end
