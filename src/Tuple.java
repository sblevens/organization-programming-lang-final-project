/* 
 * File: Tuple.java
 * Date: Spring 2022
 * Auth: Sami Blevens
 * Desc: Tuple Class
 */


public class Tuple<X,Y> {
   public final X first;
   public final Y second;
   public Tuple(X first, Y second) {
      this.first = first;
      this.second = second;
   }

   @Override
   public String toString() {
      return "(" + first + ", " + second + ")";
   }

}