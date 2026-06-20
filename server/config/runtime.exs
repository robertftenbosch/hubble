import Config

# Read PORT at boot rather than at build time so a single release can be reused
# across environments. config/config.exs still handles dev/test compile-time.
if config_env() == :prod do
  config :hubble, :port, String.to_integer(System.get_env("PORT") || "4000")
end
