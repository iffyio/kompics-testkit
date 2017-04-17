### Starting a testcase
A ```TestContext<? extends ComponentDefinition>``` reference is created using the ```Testkit.newTestContext()``` method. 
This methods takes as arguments the ComponentDefinition and Init of the component under test. Its the object implementing the builder Api. The component under test is 
accessed by calling ```tc.getComponentUnderTest()``` where tc is the test context reference.

### Creating other components and connecting
```tc.create(Class<? extends ComponentDefinition> Init)``` creates and returns a new component for the specified class.  
```tc.connect(...)``` signatures match the method signatures in Kompics Core. When connecting ports of two components other than the
component under test, the matching method is called directly from Kompics.


### Blocks
A Block is everything between a ```repeat(int)``` and a matching ```end()``` method.  
Each block has a header and a body that is explicitly defined with ```body()```.
The header consists of all methods between the ```repeat()``` and ```body()``` while the body methods the rest up to ```end()```.
Nested blocks are possible but only within the body of another block.  
A single testcase is run within a ```repeat(1)``` block but neither this nor the matching ```end()``` is specified by the user. However the ```body()``` must be called.  
For example
```
tc.body()
```
is a valid testcase albeit an empty one.

Most methods are only allowed either in the header or body.  
  
These methods are allowed exlusively within a header.  
create, connect, allow, disallow, drop, addComparator, setDefaultAction, blockExpect, onEachIteration
  
The others are allowed within a body only
all expect variants (except for blockExpect), all trigger variants, body, end, unordered, setMapperForNext, expectFault, inspect

#### Keeping loop state
The repeat() method takes a count and an option ```BlockInit``` object. This object has a single method ```init()``` that is
 called once on entering the block. Another optional method that can appear inside a block header when looping is the ```onEachIteration(BlockInit)``` which is called
 on each iteration of the loop. For example say we have some counter at zero and an object extending ```BlockInit``` that increments it by one
 every time it's called. At the end of the block the counter should be 121.

```java
 repeat(10, increment) // +1
     .onEachIteration(increment) // + 10
 .body()
     repeat(10, increment) // +10
         .onEachIteration(increment) // +10
     .body()
     .end()
 .end()
```
 
### Comparing events and default Action
The ```addComparator(Class<E>, Comparator<E>)``` method sets a default comparator to match expected events with received events assignable to class E. 
The most specific such comparator is used for events in case of multiple matches. If no comparator is found, ```equals()``` is used.  
```setDefaultAction(Class<E>, Function<E, Action>)``` is used as catch-all cases of event assignable to E that 
couldn't be matched at the current state. If no matching function was set, the test case fails. The user specified function returns
an enum Action{HANDLE, DROP, FAIL} to handle/drop the event or fail the test case if necessary.




### Triggering events
Using the ```trigger(KompicsEvent, Port)``` method. Triggers can be done on any port. However, one limitation is that it 
isn't possible (or necessary) to ```expect()``` an event that was triggered directly on the outside port of the component under test since these can't be 
intercepted. Triggers are implemented as an entry in the state table to simply trigger on the port and move on to the next state.

### Expecting events
All expect variants including those involving multiple events. are implemented as a single state. (except for blockExpect which isn't implemented as a state itself).

### Expecting Single Events
```expect(KompicsEvent, Port, Direction)``` expects a single event. Direction is either incoming or outgoing from the
perspective of the component under test. Port should be the outside port of the component under test.  
Alternatively, ```expect(Class<E>, Predicate<E>, Port, Direction)```  matches a received event if the predicate returns true.  

#### Expecting unordered events
Single event expect calls can be grouped within an ```unordered()``` and a matching ```end()``` block.
For example [see UnorderedTest.java](https://github.com/iffyio/kompics-testkit/blob/master/src/test/java/se/sics/kompics/testkit/UnorderedEventsTest.java)
A matching ```end()``` is required. Order doesn't matter when expecting events within this block. This block can not be empty.

#### Expecting within block
```blockExpect(KompicsEvent, Port, Direction)``` specifies that the event must be received exactly once
within this (and possibly nested) block. It is allowed only inside headers of block since it is not a state itself.
If some events have'nt yet been received by the end of the block, the test waits for new events and fails if the next
received event doesn't match any pending events.

### Expecting with response
Here outgoing events are expected and either and a response can be provided immediately.  
There are two modes to do this: Using a mapper Function or a (Currently custom) Future. Each has 
variant(s) of expect methods that are only allowed within that context.  

#### Mapper  
Events are specified within ```expectWithMapper()``` and ```end()```.  
for each expected event, a mapper can be specified in two ways; Implicitly via the  
```setMapperForNext(int N, Class<E>, Function<E, R> mapper)``` method, where E is the request type and
R is response type. The mappper function specified is used for the next N, expected events without mappers where N has also 
been specified. An expected event without a mapper has the signature ```expect(listenPort, responsePort)``` for outgoing
and incoming ports respectively. The other route is explicit via the method 
```expect(Class<E>, Port listenPort, Port responsePort, Function<E, R> mapper)```.  
An exception is thrown if mapper if not specified in either of these ways; for example if the implicit form is used without
a matching setMapper call or if there have already been N implicit calls between the last setMapper and this call.

Here's a correct example. [See also ExpectWithResponseTest.java](https://github.com/iffyio/kompics-testkit/blob/master/src/test/java/se/sics/kompics/testkit/ExpectWithResponseTest.java)

```java
expectWithMapper().
    setMapperForNext(2, ClassX, XMapper). // implicit mapper for next 2
    expect(portB, responsePortB).
    expect(portA, responsePortA).
    expect(ClassY, portC, portD, anotherMapper). // explicit

    setMapperForNext(1, ClassY, anotherMapper). // equivalent to explicit form
    expect(portC, portD).
end().
```

#### Future
Here the user provides a (Custom) Future object with a set and get method.  
This process is similar to the mapper in grouping expected events between ```expectWithFuture()```
and ```end()``` block. Expecting an event within this block takes the form
```expect(Class<E>, Port listenPort, Future<E, R> future)``` where E is the request class and R is response class.  
When an event is received, the ```set(E)``` method on the future is called with the event.  
Responses can optionally be triggered in any order using this same future object and  
the ```trigger(Port, Future)``` method. An error is thrown if 
the future used in a trigger has not been used in a previous expect.

Here's an example. [See also ExpectWithResponseTest.java](https://github.com/iffyio/kompics-testkit/blob/master/src/test/java/se/sics/kompics/testkit/ExpectWithResponseTest.java)

```java
expectWithFuture().
    expect(ClassX, portA, future1). // this is never triggered
    expect(ClassX, portB, future2).
    
    // possible to interleave expects/trigger
    trigger(portB, future2).
    expect(ClassY, portC, future3).
    trigger(portC, future3).
end()    
```

### Fault
Tests fault handler of the tested component by causing the component to throw a fault under some input.  
```expectFault(Class<? extends Throwable> expectedFaultType, ResolveAction)``` is specified to assert the class of
fault thrown and the resolve action to be taken respectively. Alternatively
```expectFault(Predicate<Throwable>, ResolveAction)``` can also be specified where the
specified predicate returns true on successful assertion. If the thrown fault doesn't match
the expected fault type or predicate returns false, the test case fails.  
Both methods must be preceeded by a single event expect, or a trigger as a input causing the fault otherwise an error is thrown.
[See ExpectFaultTest.java](https://github.com/iffyio/kompics-testkit/blob/master/src/test/java/se/sics/kompics/testkit/ExpectFaultTest.java)

### Allowing, Disallowing, Dropping events
Three methods ```allow(KompicsEvent, Port, Direction)```, ```disallow(KompicsEvent, Port, Direction)```,
```drop(KompicsEvent, Port, Direction)``` set constraints to allow, disallow and drop messages respectively.  
These are only allowed (pun intended) inside a header and applies to the entire (and nested) block.  
Allowed events are optionally received as opposed to expected events.  
Dropped events are also optionally receieved. However, they are not handled by the component (incoming) or forwarded to other
components (outgoing).  
Disallowed events cause the test case to fail if received.  
These constraints can be shadowed within nested blocks. For example  

```java
    repeat(3)
        .disallow(EventA, PortA, INCOMING)
        .body()
        // EventA is disallowed
        .repeat(2)
            .allow(EventA, PortA, INCOMING)
        .body()
        // EventA is allowed
        .end()
        // EventA is disallowed
    .end();
```
In case of conflicting constraints, only the last placed constraint is valid.  

### Asserting component state
Whitebox testing can be done on the component using the ```inspect(Predicate<ComponentDefinition>)``` function.
This specifies a predicate that would eventually be called with the component under test. The (currently public only) fields of 
the component may then be asserted. This is also implemented as a single state. All messages until that state would have 
been handled before the predicate is called. That is, the component has no pending events to be handled. Returning false from this
predicate causes the test case to fail.
[See AssertcomponentTest.java](https://github.com/iffyio/kompics-testkit/blob/master/src/test/java/se/sics/kompics/testkit/AssertComponentTest.java)

### Running the test
the ```check()``` method is called after all setup to run the test. It currently returns the state the test ends up in, which 
be verified as an accepting state via the ```getFinalState()``` .
