

def command = "mvn clean compile"

def proc = command.execute();

def out = proc.in.text.split("\n");

def files = new HashSet<String>();

out.each { line ->
    if(!(line.contains("ERROR") && line.contains(".java")))
        return;

    line = line.replace("[ERROR] ", "");
    line = line.substring(0, line.indexOf(':'));
    files.add(line);
}


files.each { name ->

    if(!name.endsWith(".java")) {
        return;
    }

    def file = new File(name);

    fileText = file.text;
    fileText = fileText.replaceAll("remove([a-zA-Z]+)\\(\\)", { Object[] captured ->
        String slotName = captured[0].substring(6);
        slotName = slotName.substring(0, slotName.length() - 2);
        "set" + slotName + "(null)";
    });

    fileText = fileText.replaceAll("get([a-zA-Z]+)Count\\(\\)", { Object[] captured ->
        String slotName = captured[0].substring(0, captured[0].length() - 7);
        slotName + "().size()";
    });

    fileText = fileText.replaceAll("get([a-zA-Z]+)Set\\(\\)", { Object[] captured ->
        String slotName = captured[0].substring(0, captured[0].length() - 5);
        slotName + "()";
    });

    def safe = args.size() > 0;

    if(!safe) {
        fileText = fileText.replaceAll("has([A-Z][a-zA-Z]*)\\(\\)", { Object[] captured ->
            String slotName = captured[0].substring(3);
            println slotName
            if(slotName.startsWith("Next"))
                return captured[0]
            int parenIndex = slotName.indexOf('(');
            "(get"+  slotName.substring(0, parenIndex) + "() != null)";
        });
    }

    fileText = fileText.replaceAll("has([A-Z][a-zA-Z]*)\\([A-Za-z]+\\)", { Object[] captured ->
        String slotName = captured[0].substring(3);
        int parenIndex = slotName.indexOf('(');
        "get"+  slotName.substring(0, parenIndex) + "().contains" + slotName.substring(parenIndex);
    });

    fileText = fileText.replaceAll("hasAny([a-zA-Z]*)\\(\\)", { Object[] captured ->
        String slotName = captured[0].substring(6);
        "(!get" + slotName + ".isEmpty())"
    });

    fileText = fileText.replaceAll("get([a-zA-Z]+)Iterator\\(\\)", { Object[] captured ->
        String slotName = captured[0].substring(0, captured[0].length() - 10);
        slotName + "().iterator()";
    });


    file.write(fileText);
}

