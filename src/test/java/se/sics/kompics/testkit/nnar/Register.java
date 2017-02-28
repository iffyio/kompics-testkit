package se.sics.kompics.testkit.nnar;

import java.io.Serializable;

public class Register implements Serializable{
  final int ts;
  final int writeRank;
  final Integer val;

  public Register(int ts, int writeRank, Integer val) {
    this.ts = ts;
    this.writeRank = writeRank;
    this.val = val;
  }

  boolean greaterThan(Register r) {
    return ts > r.ts || ts >= r.ts && writeRank > r.writeRank;
  }

  public boolean equals(Object o) {
    if (!(o instanceof Register)) {
      return false;
    }

    Register other = (Register) o;
    return ts == other.ts &&
           writeRank == other.writeRank
           && val.equals(other.val);
  }

  public int hashCode() {
    int result = 31 * ts;
    result += 31 * writeRank;
    result += 31 * val;
    return result;
  }
}
