/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

#include "jnienv.h"


#include <cstdlib>

static JavaVM *s_java_vm = nullptr;

JNIEnv *getJniEnv() {
    JNIEnv *env = nullptr;

    switch (s_java_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6)) {
        case JNI_OK:
            return env;
        case JNI_EDETACHED: {
            JavaVMAttachArgs args;
            args.name = nullptr;
            args.group = nullptr;
            args.version = JNI_VERSION_1_6;

            if (!s_java_vm->AttachCurrentThreadAsDaemon(&env, &args)) {
                return env;
            }
            break;
        }
    }
    return nullptr;
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *aJavaVM, void *aReserved) {
    (void) aReserved;
    s_java_vm = aJavaVM;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *aJavaVM, void *aReserved) {
    (void) aJavaVM;
    (void) aReserved;
    s_java_vm = nullptr;
}
