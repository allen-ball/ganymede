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
 * The following should be added programmatically.
 * See NotebookContext.getNotebookFunctions().
 */
fun display(obj: Any): Unit {
    val context: ganymede.notebook.NotebookContext = bindings["$$"] as ganymede.notebook.NotebookContext

    context.display(obj)
}

fun print(obj: Any): Unit {
    val context: ganymede.notebook.NotebookContext = bindings["$$"] as ganymede.notebook.NotebookContext

    context.print(obj)
}

fun asJson(obj: Any): com.fasterxml.jackson.databind.JsonNode {
    val context: ganymede.notebook.NotebookContext = bindings["$$"] as ganymede.notebook.NotebookContext

    return context.asJson(obj)
}

fun asYaml(obj: Any): String {
    val context: ganymede.notebook.NotebookContext = bindings["$$"] as ganymede.notebook.NotebookContext

    return context.asYaml(obj)
}
