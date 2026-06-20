defmodule Hubble.Heatmap.Server do
  @moduledoc """
  Holds live, anonymous presence beacons in memory and serves the coarse heatmap.
  All aggregation/privacy logic lives in the pure `Hubble.Heatmap`; this process only
  owns mutable state and a periodic sweep that drops expired beacons.

  A `now_fun` can be injected for deterministic tests.
  """
  use GenServer

  @sweep_interval_ms 60_000

  # --- Client API ------------------------------------------------------------

  def start_link(opts \\ []) do
    GenServer.start_link(__MODULE__, opts, name: Keyword.get(opts, :name, __MODULE__))
  end

  @doc "Record an anonymous beacon for a (coarse) geohash."
  def record(server \\ __MODULE__, geohash) when is_binary(geohash) do
    GenServer.cast(server, {:record, geohash})
  end

  @doc "Current heatmap cells meeting the k-anonymity floor within the TTL window."
  def heatmap(server \\ __MODULE__, opts \\ []) do
    GenServer.call(server, {:heatmap, opts})
  end

  # --- Server callbacks ------------------------------------------------------

  @impl true
  def init(opts) do
    now_fun = Keyword.get(opts, :now_fun, &System.system_time/1)
    schedule_sweep()
    {:ok, %{beacons: [], opts: opts, now_fun: now_fun}}
  end

  @impl true
  def handle_cast({:record, geohash}, state) do
    cell = Hubble.Heatmap.normalize_geohash(geohash, state.opts)
    beacon = %{geohash: cell, at_ms: now_ms(state)}
    {:noreply, %{state | beacons: [beacon | state.beacons]}}
  end

  @impl true
  def handle_call({:heatmap, opts}, _from, state) do
    merged = Keyword.merge(state.opts, opts)
    {:reply, Hubble.Heatmap.cells(state.beacons, now_ms(state), merged), state}
  end

  @impl true
  def handle_info(:sweep, state) do
    schedule_sweep()
    {:noreply, %{state | beacons: Hubble.Heatmap.expire(state.beacons, now_ms(state), state.opts)}}
  end

  defp now_ms(%{now_fun: f}), do: f.(:millisecond)
  defp schedule_sweep, do: Process.send_after(self(), :sweep, @sweep_interval_ms)
end
