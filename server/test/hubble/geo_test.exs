defmodule Hubble.GeoTest do
  use ExUnit.Case, async: true

  test "encodes a known reference location to the canonical geohash" do
    # Reference vector: 57.64911, 10.40744 -> "u4pruydqqvj" (Wikipedia geohash example).
    assert Hubble.Geo.encode(57.64911, 10.40744, 11) == "u4pruydqqvj"
  end

  test "encoding is hierarchical: shorter precision is a prefix of longer" do
    long = Hubble.Geo.encode(52.3702, 4.8952, 9)
    short = Hubble.Geo.encode(52.3702, 4.8952, 5)
    assert String.starts_with?(long, short)
  end

  test "coarsen truncates to a prefix" do
    assert Hubble.Geo.coarsen("u4pruydqqvj", 6) == "u4pruy"
  end

  test "nearby points share a coarse cell but may differ at fine precision" do
    a = Hubble.Geo.encode(52.37010, 4.89520, 6)
    b = Hubble.Geo.encode(52.37015, 4.89525, 6)
    assert a == b
  end
end
