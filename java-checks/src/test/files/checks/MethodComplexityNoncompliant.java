public class HelloWorld {

  public void sayHello() { // Noncompliant [[effortToFix=1;sc=15;ec=23;secondary=3,4]] {{The Cyclomatic Complexity of this method "sayHello" is 2 which is greater than 1 authorized.}}
    while (false) {
    }
  }

}
