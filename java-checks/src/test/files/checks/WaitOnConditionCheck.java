import java.util.concurrent.locks.Condition;

class A {
  void foo() {
    new C().wait();
    new C().wait(1);
    new C().wait(1, 3);
    new B().wait();  // Noncompliant {{The "Condition.await(...)" method should be used instead of "Object.wait(...)"}}
    new B().wait(1);  // Noncompliant
    new B().wait(1, 3);  // Noncompliant
  }

  class B implements Condition {

  }
  class C {
  }
}
