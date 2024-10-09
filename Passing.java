import javax.swing.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.HashSet;

class Passing implements ActionListener {
    JFrame f;
    JButton b1, b2;
    JTextArea t1, t2, inputArea, optabArea;
    JLabel label1, label2, labelInput, labelOptab;
    StringBuilder objectCodeBuilder;
    String result = "";

    Passing() {
        f = new JFrame("PASS ASSEMBLER");

        // Create input areas
        inputArea = new JTextArea();
        optabArea = new JTextArea();
        t1 = new JTextArea();
        t2 = new JTextArea();

        label1 = new JLabel("Intermediate/Final Output");
        label2 = new JLabel("Symbol/Object Code Table");
        labelInput = new JLabel("Input Assembly Code");
        labelOptab = new JLabel("OPTAB Entries");

        b1 = new JButton("PASS1");
        b2 = new JButton("PASS2");

        
        labelInput.setBounds(30, 10, 200, 30);
        inputArea.setBounds(30, 40, 600, 100);

        labelOptab.setBounds(30, 150, 200, 30);
        optabArea.setBounds(30, 180, 600, 100);

        label1.setBounds(30, 290, 200, 30);
        t1.setBounds(30, 320, 600, 200);

        label2.setBounds(700, 10, 200, 30);
        t2.setBounds(700, 40, 600, 480);

        b1.setBounds(40, 530, 100, 40);
        b2.setBounds(150, 530, 100, 40);

        
        f.add(labelInput);
        f.add(inputArea);
        f.add(labelOptab);
        f.add(optabArea);
        f.add(label1);
        f.add(label2);
        f.add(t1);
        f.add(t2);
        f.add(b1);
        f.add(b2);

        f.setLayout(null);
        f.setVisible(true);
        f.setSize(1400, 650);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        b1.addActionListener(this);
        b2.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == b1) {
            label1.setText("Intermediate File Content (Pass 1)");
            label2.setText("Symbol Table Content (Pass 1)");
            runPass1();
        } else if (e.getSource() == b2) {
            label1.setText("Final Code (Pass 2)");
            label2.setText("Object Code Records (Pass 2)");
            runPass2();
        }
    }

    public void runPass1() {
        HashSet<String> symtabSet = new HashSet<>();
        String inputText = inputArea.getText();
        String optabText = optabArea.getText();

        if (inputText.isEmpty() || optabText.isEmpty()) {
            t1.setText("Please provide input assembly code and OPTAB entries.");
            return;
        }

        try {
            String[] inputLines = inputText.split("\n");
            String[] optabLines = optabText.split("\n");

            StringBuilder intermediateContent = new StringBuilder();
            StringBuilder symtabContent = new StringBuilder();
            int locctr = 0, addr = 0;
            String label, opcode, operand;
            int flag = 0;

            for (String line : inputLines) {
                String[] parts = line.split("\\s+");
                label = parts[0];
                opcode = parts[1];
                operand = parts.length > 2 ? parts[2] : "";

                if (opcode.equals("START")) {
                    locctr = Integer.parseInt(operand, 16);
                    addr = locctr;
                    intermediateContent.append(String.format("-\t%s\t%s\t%s\n", label, opcode, operand));
                    continue;
                }

                intermediateContent.append(String.format("%04X\t%s\t%s\t%s\n", locctr, label, opcode, operand));

                if (!label.equals("-") && !symtabSet.contains(label)) {
                    flag = 0;
                    symtabContent.append(String.format("%s\t%04X\t%d\n", label, locctr, flag));
                    symtabSet.add(label);
                }

                if (!opcode.equals("WORD") && !opcode.equals("RESW") && !opcode.equals("RESB") && !opcode.equals("BYTE") && !opcode.equals("END")) {
                    int length = locctr - addr;
                    result = Integer.toHexString(length).toUpperCase();
                }


                if (opcode.equals("WORD")) {
                    locctr += 3;
                } else if (opcode.equals("RESW")) {
                    locctr += 3 * Integer.parseInt(operand);
                } else if (opcode.equals("RESB")) {
                    locctr += Integer.parseInt(operand);
                } else if (opcode.equals("BYTE")) {
                    locctr += operand.length() - 3;
                } else {
                    locctr += 3;
                }
            }

            t1.setText(intermediateContent.toString());
            t2.setText(symtabContent.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
            t1.setText("Error in PASS1: " + ex.getMessage());
        }
    }

    public void runPass2() {
        try {
            String optabText = optabArea.getText();
            String symtabText = t2.getText();
            String intermediateText = t1.getText();

            if (optabText.isEmpty() || symtabText.isEmpty() || intermediateText.isEmpty()) {
                t1.setText("Missing OPTAB, SYMTAB, or Intermediate code data.");
                return;
            }

            
            HashMap<String, String> optab = new HashMap<>();
            String[] optabLines = optabText.split("\n");
            for (String line : optabLines) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) {
                    optab.put(parts[0], parts[1]);
                }
            }

            
            HashMap<String, String> symtab = new HashMap<>();
            String[] symtabLines = symtabText.split("\n");
            for (String line : symtabLines) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    symtab.put(parts[0], parts[1]);
                }
            }

    
            String[] intermediateLines = intermediateText.split("\n");
            StringBuilder finalContent = new StringBuilder();
            StringBuilder objectCodeRecords = new StringBuilder();
            String startAddress = "";
            String textRecord = "";

            for (String line : intermediateLines) {
                String[] parts = line.split("\\s+");
                if (parts.length < 4) continue;

                String address = parts[0];
                String label = parts[1];
                String opcode = parts[2];
                String operand = parts[3];
                String objectCode = "";

                if (opcode.equals("START")) {
                    startAddress = operand;
                    finalContent.append(String.format("%s\t%s\t%s\n", label, opcode, operand));
                    objectCodeRecords.append(String.format("H^%s^%s^%06X\n", label, operand, Integer.parseInt(result, 16)));
                    continue;
                }

                if (opcode.equals("WORD")) {
                    objectCode = String.format("%06X", Integer.parseInt(operand));
                } else if (opcode.equals("BYTE")) {
                    if (operand.startsWith("C'")) {
                        StringBuilder asciiValue = new StringBuilder();
                        for (int i = 2; i < operand.length() - 1; i++) {
                            asciiValue.append(String.format("%02X", (int) operand.charAt(i)));
                        }
                        objectCode = asciiValue.toString();
                    }
                } else if (optab.containsKey(opcode)) {
                    String opcodeValue = optab.get(opcode);
                    String operandAddress = symtab.getOrDefault(operand, "0000");
                    objectCode = String.format("%s%s", opcodeValue, operandAddress);
                }

                if (!objectCode.isEmpty()) {
                    textRecord += "^" + objectCode;
                }

                finalContent.append(String.format("%s\t%s\t%s\t%s\t%s\n", address, label, opcode, operand, objectCode));
            }

            
            objectCodeRecords.append(String.format("T^%s%s\n", startAddress, textRecord));
            objectCodeRecords.append(String.format("E^%s\n", startAddress));

            t1.setText(finalContent.toString());
            t2.setText(objectCodeRecords.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
            t1.setText("Error in PASS2: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        new Passing();
    }
}
