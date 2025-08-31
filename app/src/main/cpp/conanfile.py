import os
import subprocess

from conan import ConanFile
from conan.tools.cmake import CMakeToolchain, CMake, cmake_layout


class FptnLib(ConanFile):
    name = "fptn-lib"
    version = "0.0.0"
    requires = ("nlohmann_json/3.12.0",)
    settings = (
        "os",
        "arch",
        "compiler",
        "build_type",
    )
    generators = ("CMakeDeps",)
    default_options = {
        "*:fPIC": True,
        "*:shared": False,
        # libfptn options
        "fptn/*:build_only_fptn_lib": True,
        "fptn/*:with_gui_client": False,
    }

    def requirements(self):
        self._register_local_recipe("fptn", "fptn", "0.0.0")

    def layout(self):
        cmake_layout(self)

    def generate(self):
        tc = CMakeToolchain(self)
        # setup fptn
        fptn_dep = self.dependencies["fptn"]
        tc.variables["FPTN_INCLUDE_DIR"] = fptn_dep.cpp_info.includedirs[0]
        tc.variables["FPTN_LIBRARY"] = fptn_dep.cpp_info.libs[0]
        tc.variables["FPTN_LIBRARY_DIR"] = fptn_dep.cpp_info.libdirs[0]

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
        recipe_rel_path = os.path.join(script_dir, "libs", "fptn")
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
