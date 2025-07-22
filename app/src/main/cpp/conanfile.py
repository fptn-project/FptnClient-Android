import os
import subprocess

from conan import ConanFile
from conan.tools.cmake import CMakeToolchain, CMake, cmake_layout


# CI will replace this automatically
FPTN_VERSION = "0.0.1"


class FptnLib(ConanFile):
    name = "fptn-lib"
    version = FPTN_VERSION
    requires = (
        "zlib/1.3.1",
        "fmt/11.1.3",
        # need to update boost
        "boost/1.83.0",
        "abseil/20250127.0",
        "argparse/3.2",
        "spdlog/1.15.1",
        "protobuf/5.27.0",
        "nlohmann_json/3.12.0",
    )
    settings = (
        "os",
        "arch",
        "compiler",
        "build_type",
    )
    generators = ("CMakeDeps",)
    default_options = {
        # -- depends --
        "*:fPIC": True,
        "*:shared": False,
        # --- protobuf options ---
        "fptn/*:build_only_fptn_lib": True,  # specific option!
        "fptn/*:with_gui_client": False,
        "protobuf/*:lite": True,
        "protobuf/*:upb": False,
        "protobuf/*:with_rtti": False,
        "protobuf/*:with_zlib": False,
        "protobuf/*:debug_suffix": False,
        # --- boost options ---
        "boost/*:without_atomic": False,
        "boost/*:without_system": False,
        "boost/*:without_process": False,
        "boost/*:without_exception": False,
        "boost/*:without_container": False,
        "boost/*:without_filesystem": False,
        "boost/*:without_coroutine": False,
        "boost/*:without_context": False,
        "boost/*:without_timer": False,
        "boost/*:without_json": False,
        "boost/*:without_random": False,
        "boost/*:without_iostreams": False,
        "boost/*:without_regex": False,
        "boost/*:without_zlib": False,
        "boost/*:without_python": True,
        "boost/*:without_chrono": True,
        "boost/*:without_contract": True,
        "boost/*:without_date_time": True,
        "boost/*:without_fiber": True,
        "boost/*:without_graph": True,
        "boost/*:without_graph_parallel": True,
        # "boost/*:without_iostreams": True,
        "boost/*:without_locale": True,
        "boost/*:without_log": True,
        "boost/*:without_math": True,
        "boost/*:without_mpi": True,
        "boost/*:without_nowide": True,
        "boost/*:without_program_options": True,
        # "boost/*:without_regex": True,
        "boost/*:without_serialization": True,
        "boost/*:without_stacktrace": True,
        "boost/*:without_test": True,
        "boost/*:without_thread": True,
        "boost/*:without_url": True,
        "boost/*:without_type_erasure": True,
        "boost/*:without_wave": True,
    }
    def build_requirements(self):
        self.tool_requires("protobuf/5.27.0")

    def requirements(self):
        # WE USE BORINGSSL
        self._register_local_recipe("pcapplusplus", "pcapplusplus", "24.09")
        self._register_local_recipe("boringssl", "openssl", "boringssl", override=False, force=False)

    def layout(self):
        cmake_layout(self)

    def generate(self):
        tc = CMakeToolchain(self)
        protobuf_build = self.dependencies.build["protobuf"]
        protoc_path = os.path.join(protobuf_build.package_folder, "bin", "protoc")
        tc.cache_variables["Protobuf_PROTOC_EXECUTABLE"] = protoc_path
        print("Protoc Path: ", protobuf_build, protoc_path)

        tc.variables["BUILD_ANDROID"] = True
        tc.cache_variables["Protobuf_PROTOC_EXECUTABLE"] = protoc_path
        tc.cache_variables["Protobuf_ROOT"] = protobuf_build.package_folder
        tc.cache_variables["Protobuf_USE_STATIC_LIBS"] = True

        tc.generate()

    def build(self):
        cmake = CMake(self)
        cmake.configure()
        cmake.build()

    def config_options(self):
        if self.settings.os == "Windows":
            self.options.rm_safe("fPIC")

    def _register_local_recipe(self, recipe, name, version, override=False, force=False):
        script_dir = os.path.dirname(os.path.abspath(__file__))
        recipe_rel_path = os.path.join(script_dir, "libs", "fptn", ".conan", "recipes", recipe)
        subprocess.run(
            [
                "conan",
                "export",
                recipe_rel_path,
                f"--name={name}",
                f"--version={version}",
                "--user=local",
                "--channel=local",
            ],
            check=True,
        )
        self.requires(f"{name}/{version}@local/local", override=override, force=force)
