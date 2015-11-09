package io.github.andrebeat.pool

/**
  * A `ConcurrentBag` is a a thread-safe, unordered collection that allows duplicates and that
  * provides add and get operations. The implementation is optimized for scenarios where the same
  * thread will be both producing and consuming data stored in the bag.
  *
  * This data structure originated in .NET and this implementation borrows from the original
  * implementation [1]. Additionally, the general description of the data structure and its
  * operations thats given below is mostly based on (and adapted) a blog post from Simon Cooper [2].
  *
  * `ConcurrentBag`, at its core, operates as a linked list of linked lists. Each thread maintains a
  * reference to the head of a linked-list stored in a thread-local variable
  * (`ThreadLocalList`). The bag maintains pointers (shared with other threads, not thread-local) to
  * the head and tail list, i.e. the head list stored in a thread-local variable of thread A and the
  * last list stored in a thread-local variable of thread Z. It is important to note that although
  * the head pointer to the thread-local list cannot be accessed by other threads (since it is
  * thread-local), it is only that case if the `Node` is accessed through that variable
  * (pointer). Since we maintain global pointers to head and tail of the lists it is possible for
  * any thread to reach each others thread-local lists. Additionally, the `ThreadLocalList`
  * maintains a pointer to the next list (stored in another thread).
  *
  * ==Operations on thread-local lists==
  * ===Adding items===
  * - If the current thread doesn't have an instance of a thread-local list, create one. Then add
  *   the item to the head of the list. (Optionally reclaim lists from other threads that are no
  *   longer running).
  *
  * ===Taking & Peeking===
  * - If the current thread's local list has items, then peek or remove the *head* item.
  * - If the local list is empty iterate through other thread-local lists until it finds an item.
  *   - If there are 3 or more items in the list, take or peek tail item
  *   - Else, use a lock on the thread-local list to mediate access
  *
  * ===Thread Synchronization===
  * - Operations performed by the owner thread only use the lock when there are less then 3 items in
  *   the list.
  * - Otherwise, the owning thread sets the list `currentOp` variable to a non-zero value during the
  *   operation. This indicates to all other threads that there is a non-locked operation currently
  *   occuring on that list.
  * - The stealing thread always uses the lock, to prevent two threads trying to steal from the same
  *   list at the same time.
  * - After taking out the lock, the stealing thread spinwaits until `currentOp` has been set to
  *   zero before actually performing the steal. This ensures there won't be a conflict with the
  *   owning thread when the number of items in the list is on the 2-3 item borderline (if any add
  *   or remove operations are started in the meantime, and the list is below 3 items, those
  *   operations will be blocked waiting on the lock acquired by the currenly stealing thread).
  *
  * ===Global operations===
  * Whenever a thread operates on anything outside of its thread-local list it must use the
  * `globalLock` variable. For adding items taking this lock is enough since its only operating on
  * the "global" structure (not other thread's lists), i.e. the global head and tail pointers.
  *
  * ====Freezing====
  * For operations like `count` or `toArray` it is necessary to make sure that all other operations
  * on the bag are stopped in order to ensure a consistent view of the whole data structure.
  * In order to "freeze" a bag:
  * - Take the global lock;
  * - `needSync` is set to true. This makes sure that all the threads will *always* take the local
  *   lock when operating on their lists;
  * - Take out all the lists locks in order (this blocks all other operations on the lists since
  *   they now *always* require locking);
  * - Wait for all current lockless operations to finish by spinwaiting on each list's `currentOp` field.
  *
  * The bag is now frozen and it is now safe to traverse the whole data-structure and perform any
  * arbitrary operations on it. Afterwards, release all the locks (list's and global) and set
  * `needSync` back to false.
  *
  * [1]: https://github.com/dotnet/corefx/blob/master/src/System.Collections.Concurrent/src/System/Collections/Concurrent/ConcurrentBag.cs
  * [2]: https://www.simple-talk.com/blogs/2012/03/26/inside-the-concurrent-collections-concurrentbag/
  *
  */
class ConcurrentBag[A <: AnyRef] {

  private class Node(
    val value: A,
    var next: Node = null,
    var prev: Node = null
  )

  private class ThreadLocalList(private[pool] var ownerThreadId: Int) {

    // Head node in the list, null means the list is empty
    @volatile private[pool] var head: Node = _

    // Tail node for the list
    @volatile private[this] var tail: Node = _

    // The current list operation
    @volatile private[pool] var currentOp: ListOperation = ListOperation.None

    // The list count from the Add/Take perspective
    private[this] var count: Int = _

    // The stealing count
    private[this] var stealCount: Int = _

    // Next list in the dictionary values
    @volatile private[pool] var nextList: ThreadLocalList = _

    // Set if the local lock is taken
    private[pool] var lockTaken: Boolean = _

    // the version of the list, incremented only when the list changed from empty to non empty state
    @volatile private[pool] var version: Int = _

    def add(a: A, updateCount: Boolean) {
      count += 1
      var node = new Node(a)

      if (head eq null) {
        assert(tail eq null)

        head = node
        tail = node
        version += 1 // changing from empty state to non empty state
      } else {
        node.next = head
        head.prev = node
        head = node
      }

      if (updateCount) { // update the count to avoid overflow if this add is synchronized
        count = count - stealCount
        stealCount = 0
      }
    }

    def remove(): A = {
      assert(head ne null)

      val hd = head
      head = head.next

      if (head ne null) {
        head.prev = null
      } else {
        tail = null
      }

      count -= 1
      hd.value
    }

    def peek(): Option[A] =
      Option(head).map(_.value)

    def steal(remove: Boolean): A = {
      assert(tail ne null)

      val tl = tail

      if (remove) { // Take operation
        tail = tail.prev

        if (tail ne null) {
          tail.next = null
        } else {
          head = null
        }

        // Increment the steal count
        stealCount += 1
      }

      tl.value
    }

    def count(): Int = count - stealCount
  }

  private sealed trait ListOperation
  private object ListOperation {
    case object None extends ListOperation
    case object Add extends ListOperation
    case object Take extends ListOperation
  }
}
