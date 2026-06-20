defmodule Hubble.Relay.Server do
  @moduledoc """
  Holds store-and-forward mailboxes in memory and serves them. All policy lives in the
  pure `Hubble.Relay`; this process owns mutable state and a periodic expiry sweep.
  `now_fun` is injectable for deterministic tests.
  """
  use GenServer

  @sweep_interval_ms 3_600_000

  def start_link(opts \\ []) do
    GenServer.start_link(__MODULE__, opts, name: Keyword.get(opts, :name, __MODULE__))
  end

  @doc "Deposit an opaque envelope for a recipient mailbox."
  def deposit(server \\ __MODULE__, mailbox_id, data) when is_binary(data) do
    GenServer.cast(server, {:deposit, mailbox_id, data})
  end

  @doc "Collect (and remove) all pending envelopes for a mailbox."
  def collect(server \\ __MODULE__, mailbox_id) do
    GenServer.call(server, {:collect, mailbox_id})
  end

  @impl true
  def init(opts) do
    now_fun = Keyword.get(opts, :now_fun, &System.system_time/1)
    schedule_sweep()
    {:ok, %{mailboxes: %{}, opts: opts, now_fun: now_fun}}
  end

  @impl true
  def handle_cast({:deposit, mailbox_id, data}, state) do
    {:noreply, %{state | mailboxes: Hubble.Relay.put(state.mailboxes, mailbox_id, data, now_ms(state))}}
  end

  @impl true
  def handle_call({:collect, mailbox_id}, _from, state) do
    {blobs, mailboxes} = Hubble.Relay.take(state.mailboxes, mailbox_id, now_ms(state), state.opts)
    {:reply, blobs, %{state | mailboxes: mailboxes}}
  end

  @impl true
  def handle_info(:sweep, state) do
    schedule_sweep()
    {:noreply, %{state | mailboxes: Hubble.Relay.expire(state.mailboxes, now_ms(state), state.opts)}}
  end

  defp now_ms(%{now_fun: f}), do: f.(:millisecond)
  defp schedule_sweep, do: Process.send_after(self(), :sweep, @sweep_interval_ms)
end
