/*-
 * ##########################################################################
 * Ganymede
 * %%
 * Copyright (C) 2021, 2022 Allen D. Ball
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
def printf(format: String, args: Any*): Unit = {
    Console.withOut(System.out) { Console.out.printf(format, args) }
}

def println(obj: Any): Unit = {
    Console.withOut(System.out) { Console.out.println(obj) }
}

def println(): Unit = {
    Console.withOut(System.out) { Console.out.println() }
}
/*
 * The following should be added programmatically.
 * See NotebookContext.getNotebookFunctions().
 */
def display(obj: Any): Unit = {
    $ctx.$$.asInstanceOf[_root_.ganymede.notebook.NotebookContext].display(obj)
}

def print(obj: Any): Unit = {
    $ctx.$$.asInstanceOf[_root_.ganymede.notebook.NotebookContext].print(obj)
}

def asJson(obj: Any): _root_.com.fasterxml.jackson.databind.JsonNode = {
    $ctx.$$.asInstanceOf[_root_.ganymede.notebook.NotebookContext].asJson(obj)
}

def asYaml(obj: Any): String = {
    $ctx.$$.asInstanceOf[_root_.ganymede.notebook.NotebookContext].asYaml(obj)
}
