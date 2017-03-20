import java.io.IOException;

public class VirtualMachine {

  public static final int MAX_INT = 65535;
  public VirtualMemory memory;
  public int sp;
  public int pc;

  VirtualMachine() {
    memory = new VirtualMemory(2);
    sp = 0;
    pc = 0;
  }

  public void fillMemory() throws IOException {
    String program = ExternalMemory.read(PhysicalMachine.programs.get(0), 0);
    String[] statements = program.split(";");
    String status = "begin";
    for (String statement : statements) {
      if (status.equals("begin")) {
        if (statement.equals("#DATASEG")) {
          status = "data";
          continue;
        } else {
          throw new IOException();
        }
      }
      if (status.equals("data")) {
        if (statement.equals("#CODESEG")) {
          status = "code";
          continue;
        }
        memory.getBlock(0).push(statement, sp++);
      }
      if (status.equals("code")) {
        memory.getBlock(1).put(statement, pc++);
      }
    }
    pc = 0;
  }

  //  Jeigu rezultatas netelpa, OF = 1. Jeigu reikšmės ženklo bitas yra 1, SF = 1.
  public void ADD() {
    String a = memory.getBlock(0).pop(sp--);
    int a1 = Integer.parseInt(a, 16);

    String b = memory.getBlock(0).pop(sp--);
    int b1 = Integer.parseInt(b, 16);

    if (a1 + b1 > MAX_INT) {
      PhysicalMachine.setCF();
    }
    a1 += b1;
    if (((a1 >> 6) & 1) == 1) {
      PhysicalMachine.setSF();
    }
    memory.getBlock(0).push(String.valueOf(b1), sp++);
    memory.getBlock(0).push(String.valueOf(a1), sp++);
    ++PhysicalMachine.pc;
  }

  public void SUB() {
    String a = memory.getBlock(0).pop(sp--);
    int a1 = Integer.parseInt(a, 16);

    String b = memory.getBlock(0).pop(sp--);
    int b1 = Integer.parseInt(b, 16);

    if (a1 - b1 < 0) {
      PhysicalMachine.setOF();
      return;
    } else {
      a1 -= b1;
    }
    if (((a1 >> 6) & 1) == 1) {
      PhysicalMachine.setSF();
    }
    memory.getBlock(0).push(String.valueOf(b1), sp++);
    memory.getBlock(0).push(String.valueOf(a1), sp++);
    ++PhysicalMachine.pc;
  }

  public void MUL() {
    String a = memory.getBlock(0).pop(sp--);
    int a1 = Integer.parseInt(a, 16);

    String b = memory.getBlock(0).pop(sp--);
    int b1 = Integer.parseInt(b, 16);
    if (a1 * b1 > MAX_INT) {
      PhysicalMachine.setOF();
      return;
    } else {
      a1 *= b1;
    }
    if (((a1 >> 6) & 1) == 1) {
      PhysicalMachine.setSF();
    }
    memory.getBlock(0).push(String.valueOf(b1), sp++);
    memory.getBlock(0).push(String.valueOf(a1), sp++);
    ++PhysicalMachine.pc;
  }

  // Padalina R1 iš R2, įrašoma į R1. Jeigu reikšmės ženklo bitas yra 1, SF = 1.
  public void DIV() {
    String a = memory.getBlock(0).pop(sp--);
    int a1 = Integer.parseInt(a, 16);

    String b = memory.getBlock(0).pop(sp--);
    int b1 = Integer.parseInt(b, 16);
    a1 /= b1;
    if (((a1 >> 6) & 1) == 1) {
      PhysicalMachine.setSF();
    }
    memory.getBlock(0).push(String.valueOf(b1), sp++);
    memory.getBlock(0).push(String.valueOf(a1), sp++);
    ++PhysicalMachine.pc;
  }

  //Ši komanda palygina registre R1 ir R2 ęsančias reikšmes. Jeigu reikšmės lygios, ZF = 1, priešingu atveju ZF = 0.
  public void CMP() {
    String a = memory.getBlock(0).pop(sp--);
    int a1 = Integer.parseInt(a, 16);

    String b = memory.getBlock(0).pop(sp--);
    int b1 = Integer.parseInt(b, 16);

    if (a1 == b1) {
      PhysicalMachine.setZF();
    } else {
      PhysicalMachine.clearZF();
    }
    memory.getBlock(0).push(String.valueOf(b1), sp++);
    memory.getBlock(0).push(String.valueOf(a1), sp++);
    ++PhysicalMachine.pc;
  }

  //TODO VISI JUMPAI NUŠOKA DUOTU ADRESU
  //JMx1x2 - besąlyginio valdymo perdavimo komanda. Ji reiškia, kad valdymas turi būti perduotas kodo segmento žodžiui, nurodytam adresu 16 * x1 + x2
  public void JM(String address) {
    pc = Integer.parseInt(address, 16);

  }

  //JEx1x2 - valdymas turi būti perduotas kodo segmento žodžiui, nurodytam adresu 16* x1 + x2 jeigu ZF = 1
  public void JE(String address) {
    if (PhysicalMachine.getZF() == 1) {
      pc = Integer.parseInt(address, 16);
    }

  }

  //JNx1x2 - valdymas turi būti perduotas kodo segmentui, nurodytam adresu 16*x1+x2, jeigu ZF = 0
  public void JN(String address) {
    if (PhysicalMachine.getZF() == 0) {
      pc = Integer.parseInt(address, 16);
    }

  }

  //JAx1x2 - valdymas turi būti perduotas kodo segmento žodžiui, nurodytam adresu 16* x1 + x2 jeigu CF = OF
  public void JA(String address) {
    if (PhysicalMachine.getCF() == 0) {
      pc = Integer.parseInt(address, 16);
    }
  }

  //JBx1x2 - valdymas turi būti perduotas kodo segmento žodžiui, nurodytam adresu 16* x1 + x2 jeigu CF=1
  public void JB(String address) {
    if (PhysicalMachine.getCF() == 1) {
      pc = Integer.parseInt(address, 16);
    }
  }

  //JGx1x2 - valdymas turi būti perduotas kodo segmento žodžiui, nurodytam adresu 16* x1 + x2 jeigu ZF = 0 IR SF = OF
  public void JG(String address) {
    if (PhysicalMachine.getZF() == 0 && PhysicalMachine.getSF() == PhysicalMachine.getOF()) {
      pc = Integer.parseInt(address, 16);
    }
  }

  //JLx1x2 - valdymas turi būti perduotas kodo segmento žodžiui, nurodytam adresu 16* x1 + x2 jeigu SF != OF
  public void JL(String address) {
    if (PhysicalMachine.getSF() != PhysicalMachine.getOF()) {
      pc = Integer.parseInt(address, 16);
    }
  }

  public void PUSH() {
    memory.getBlock(0).push(String.valueOf(PhysicalMachine.r), sp++);
    ++PhysicalMachine.pc;
  }

  public void POP() {
    PhysicalMachine.r = Integer.parseInt(memory.getBlock(0).pop(sp--), 16);
    ++PhysicalMachine.pc;
  }

  public void PRNL() {
    System.out.print('\n');
    ++PhysicalMachine.pc;
  }

  //TODO SUTARKYT GD IR PD
  public void GD(String x, String y) {
    Integer.parseInt(x, 16);
    Integer.parseInt(y, 16);
    ++PhysicalMachine.pc;
  }

  public void PD(String x, String y) {
    Integer.parseInt(x, 16);
    Integer.parseInt(y, 16);
    ++PhysicalMachine.pc;
  }

}