# Development shell for NixOS: provides the native libraries that Minecraft's
# bundled LWJGL loads at runtime (windowing, OpenGL, audio, input).
# Usage: nix-shell --run './gradlew :1.21.1-fabric:runClient'
{ pkgs ? import <nixpkgs> { } }:

let
  runtimeLibs = with pkgs; [
    libglvnd
    libx11
    libxcursor
    libxrandr
    libxxf86vm
    libxi
    libxext
    wayland
    libxkbcommon
    libpulseaudio
    alsa-lib
    openal
    udev
    flite
  ];
in
pkgs.mkShell {
  packages = [ pkgs.jdk25 ];

  LD_LIBRARY_PATH = "/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath runtimeLibs}";
}
