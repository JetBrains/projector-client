/*
 * MIT License
 *
 * Copyright (c) 2019-2023 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jetbrains.projector.server.core.util

public class ObjectIdCacher<IdType : Number, ObjectType : Any>(initialId: IdType, private val incrementer: (IdType) -> IdType) {

  private var nextId = initialId

  private val idToObject = mutableMapOf<IdType, ObjectType>()
  private val objectToId = mutableMapOf<ObjectType, IdType>()

  public fun getIdBy(obj: ObjectType): IdType {
    val id = objectToId[obj]

    if (id != null) {
      return id
    }

    val newId = nextId
    nextId = incrementer(nextId)

    idToObject[newId] = obj
    objectToId[obj] = newId

    return newId
  }

  public fun getObjectBy(id: IdType): ObjectType = requireNotNull(idToObject[id]) { "No id $id found. Available ids: $idToObject" }
}
