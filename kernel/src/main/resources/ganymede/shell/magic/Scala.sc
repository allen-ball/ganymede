/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
/*
 * Partial workaround for https://github.com/scala/bug/issues/11754.
 */
def print(obj: Any): Unit = {
    Console.withOut(System.out) { Console.out.print(obj) }
}

def printf(text: String, args: Any*): Unit = {
    Console.withOut(System.out) { Console.out.printf(text, args) }
}

def println(x: Any): Unit = {
    Console.withOut(System.out) { Console.out.println(x) }
}

def println(): Unit = {
    Console.withOut(System.out) { Console.out.println() }
}
