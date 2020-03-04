package qub;

public interface QubDependenciesUpdateTests
{
    static void test(TestRunner runner)
    {
        PreCondition.assertNotNull(runner, "runner");

        runner.testGroup(QubDependenciesUpdate.class, () ->
        {
            runner.testGroup("getParameters(Process)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependenciesUpdate.getParameters(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with no arguments", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create())
                    {
                        final InMemoryCharacterStream output = new InMemoryCharacterStream();
                        process.setOutputCharacterWriteStream(output);

                        final QubDependenciesUpdateParameters parameters = QubDependenciesUpdate.getParameters(process);
                        test.assertNotNull(parameters);
                        test.assertSame(output, parameters.getOutput());
                        test.assertEqual("", output.getText().await());
                        test.assertNotNull(parameters.getVerbose());
                        test.assertEqual(process.getCurrentFolder().await(), parameters.getFolder());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    try (final QubProcess process = QubProcess.create("-?"))
                    {
                        final InMemoryCharacterStream output = new InMemoryCharacterStream();
                        process.setOutputCharacterWriteStream(output);

                        final QubDependenciesUpdateParameters parameters = QubDependenciesUpdate.getParameters(process);
                        test.assertNull(parameters);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-dependencies update [--intellij] [--profiler] [--verbose] [--help]",
                                "  Update the dependencies of a project.",
                                "  --intellij: Whether or not to update IntelliJ project files.",
                                "  --profiler: Whether or not this application should pause before it is run to allow a profiler to be attached.",
                                "  --verbose(v): Whether or not to show verbose logs.",
                                "  --help(?): Show the help message for this application."
                            ),
                            Strings.getLines(output.getText().await()));
                    }
                });
            });

            runner.testGroup("run(QubDependenciesParameters)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependenciesUpdate.run(null),
                        new PreConditionFailure("parameters cannot be null."));
                });

                runner.test("with non-existing folder", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.getFolder("/project/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No project.json file found at /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with no project.json", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No project.json file found at /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no java property", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No \"java\" property found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no dependencies property", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No dependencies found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with empty dependencies property", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create()))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "No dependencies found in /project/project.json."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with no QUB_HOME environment variable", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "c"),
                                new ProjectSignature("d", "e", "f"),
                                new ProjectSignature("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables();
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Can't discover transitive dependencies if a QUB_HOME environment variable is not specified."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with relative QUB_HOME environment variable", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder folder = fileSystem.createFolder("/project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "c"),
                                new ProjectSignature("d", "e", "f"),
                                new ProjectSignature("g", "h", "i"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", "qub");
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(1, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Can't discover transitive dependencies if the QUB_HOME environment variable is not rooted."),
                        Strings.getLines(output.getText().await()));
                });

                runner.test("with project.json with three dependencies with mixed updates", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("a", "b", "2").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("2")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "3").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("3")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("g", "h", "4").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("g")
                            .setProject("h")
                            .setVersion("4")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("g", "h", "5").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("g")
                            .setProject("h")
                            .setVersion("5")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"),
                                new ProjectSignature("d", "e", "3"),
                                new ProjectSignature("g", "h", "4"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 3 dependencies:",
                            "  a/b@1 - Updated to a/b@2",
                            "  d/e@3 - No updates",
                            "  g/h@4 - Updated to g/h@5"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "2"),
                                    new ProjectSignature("d", "e", "3"),
                                    new ProjectSignature("g", "h", "5")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with one up-to-date dependency", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("d", "e", "1"))))
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with two up-to-date dependencies", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("d", "e", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("d")
                            .setProject("e")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"),
                                new ProjectSignature("d", "e", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 2 dependencies:",
                            "  a/b@1 - No updates",
                            "  d/e@1 - No updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1"),
                                    new ProjectSignature("d", "e", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with one dependency that doesn't exist", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - Not Found"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=false and one up-to-date dependency", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(false);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and no IntelliJ module files", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with no module root element", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("foo"))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  Invalid Intellij Module file: /project/project.iml"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with no component element", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module"))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with no orderEntry element", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with non-orderEntry element", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("hello"))))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with orderEntry element with no type attribute", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry"))))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with no library elements", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library"))))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with no CLASSES elements", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")))))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with no root elements", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES"))))))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with root element with no url attribute", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with root element with empty url attribute", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with root element with non-\"jar://\" url attribute", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "hello")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with root element with jar url attribute that isn't under the qub folder", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar:///other/folder/thing.jar!/")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  /other/folder/thing.jar - No updates",
                            "  a/b@1 - Added"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with up-to-date dependency with CLASSES", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - No updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "1").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with older dependency with CLASSES", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "2").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("2")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "2"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@2 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Updated to a/b@2"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "2")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "2").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "2").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with newer dependency with CLASSES", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "2").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("2")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "2"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "3").await() + "!/")))))))
                        .toString());
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@2 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@3 - Updated to a/b@2"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "2")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "2").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "2").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with up-to-date dependency with CLASSES and SOURCES", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))
                                        .addChild(XMLElement.create("JAVADOC"))
                                        .addChild(XMLElement.create("SOURCES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "1").await() + "!/")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - No updates"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "1").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with older dependency with CLASSES and SOURCES", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "2").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("2")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "2"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))
                                        .addChild(XMLElement.create("JAVADOC"))
                                        .addChild(XMLElement.create("SOURCES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "1").await() + "!/")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@2 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Updated to a/b@2"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "2")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "2").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "2").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with newer dependency with CLASSES and SOURCES", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "2").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("2")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "2"))))
                        .toString()).await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module")
                            .addChild(XMLElement.create("component")
                                .addChild(XMLElement.create("orderEntry")
                                    .setAttribute("type", "module-library")
                                    .addChild(XMLElement.create("library")
                                        .addChild(XMLElement.create("CLASSES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "3").await() + "!/")))
                                        .addChild(XMLElement.create("JAVADOC"))
                                        .addChild(XMLElement.create("SOURCES")
                                            .addChild(XMLElement.create("root")
                                                .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "3").await() + "!/")))))))
                        .toString(XMLFormat.pretty));
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@2 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@3 - Updated to a/b@2"),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "2")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "2").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "2").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                });

                runner.test("with project.json with --intellij=true and one IntelliJ project file with updated workspace.xml file", (Test test) ->
                {
                    final InMemoryCharacterStream output = new InMemoryCharacterStream();
                    final VerboseCharacterWriteStream verbose = new VerboseCharacterWriteStream(false, output);
                    final InMemoryFileSystem fileSystem = new InMemoryFileSystem(test.getClock());
                    fileSystem.createRoot("/").await();
                    final Folder rootFolder = fileSystem.getFolder("/").await();
                    final QubFolder qubFolder = QubFolder.get(rootFolder.getFolder("qub").await());
                    qubFolder.getProjectJSONFile("a", "b", "1").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("a")
                            .setProject("b")
                            .setVersion("1")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    qubFolder.getProjectJSONFile("qub", "test-java", "2").await().setContentsAsString(
                        ProjectJSON.create()
                            .setPublisher("qub")
                            .setProject("test-java")
                            .setVersion("2")
                            .setJava(ProjectJSONJava.create())
                        .toString()).await();
                    final Folder folder = rootFolder.createFolder("project/").await();
                    folder.setFileContentsAsString("project.json", ProjectJSON.create()
                        .setPublisher("x")
                        .setProject("y")
                        .setVersion("3")
                        .setJava(ProjectJSONJava.create()
                            .setDependencies(Iterable.create(
                                new ProjectSignature("a", "b", "1"))))
                        .toString())
                        .await();
                    folder.setFileContentsAsString("project.iml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("module"))
                        .toString())
                        .await();
                    folder.setFileContentsAsString(".idea/workspace.xml", XMLDocument.create()
                        .setDeclaration(XMLDeclaration.create()
                            .setVersion("1.0")
                            .setEncoding("UTF-8"))
                        .setRoot(XMLElement.create("project"))
                        .toString(XMLFormat.pretty))
                        .await();
                    folder.createFile("tests/my/CodeTests.java").await();
                    final EnvironmentVariables environmentVariables = new EnvironmentVariables()
                        .set("QUB_HOME", qubFolder.toString());
                    final QubDependenciesUpdateParameters parameters = new QubDependenciesUpdateParameters(output, verbose, folder, environmentVariables)
                        .setIntellij(true);

                    test.assertEqual(0, QubDependenciesUpdate.run(parameters));

                    test.assertEqual(
                        Iterable.create(
                            "Updating dependencies for /project/...",
                            "Found 1 dependency:",
                            "  a/b@1 - No updates",
                            "Updating IntelliJ module files...",
                            "  a/b@1 - Added",
                            "Updating IntelliJ workspace file..."),
                        Strings.getLines(output.getText().await()));
                    test.assertEqual(
                        ProjectJSON.create()
                            .setPublisher("x")
                            .setProject("y")
                            .setVersion("3")
                            .setJava(ProjectJSONJava.create()
                                .setDependencies(Iterable.create(
                                    new ProjectSignature("a", "b", "1")))),
                        ProjectJSON.parse(folder.getFile("project.json").await()).await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("module")
                                .addChild(XMLElement.create("component")
                                    .setAttribute("name", "NewModuleRootManager")
                                    .addChild(XMLElement.create("orderEntry")
                                        .setAttribute("type", "module-library")
                                        .addChild(XMLElement.create("library")
                                            .addChild(XMLElement.create("CLASSES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getCompiledSourcesFile("a", "b", "1").await() + "!/")))
                                            .addChild(XMLElement.create("JAVADOC"))
                                            .addChild(XMLElement.create("SOURCES")
                                                .addChild(XMLElement.create("root")
                                                    .setAttribute("url", "jar://" + qubFolder.getSourcesFile("a", "b", "1").await() + "!/")))))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString("project.iml").await());
                    test.assertEqual(
                        XMLDocument.create()
                            .setDeclaration(XMLDeclaration.create()
                                .setVersion("1.0")
                                .setEncoding("UTF-8"))
                            .setRoot(XMLElement.create("project")
                                .addChild(XMLElement.create("component")
                                    .setAttribute("name", "RunManager")
                                    .addChild(XMLElement.create("configuration")
                                        .setAttribute("type", "Application")
                                        .setAttribute("factoryName", "Application")
                                        .setAttribute("name", "my.CodeTests")
                                        .addChild(XMLElement.create("method")
                                            .setAttribute("v", "2")
                                            .addChild(XMLElement.create("option")
                                                .setAttribute("name", "Make")
                                                .setAttribute("enabled", "true")))
                                        .addChild(XMLElement.create("option")
                                            .setAttribute("name", "MAIN_CLASS_NAME")
                                            .setAttribute("value", "qub.ConsoleTestRunner"))
                                        .addChild(XMLElement.create("module")
                                            .setAttribute("name", "y"))
                                        .addChild(XMLElement.create("option")
                                            .setAttribute("name", "PROGRAM_PARAMETERS")
                                            .setAttribute("value", "my.CodeTests"))
                                        .addChild(XMLElement.create("option")
                                            .setAttribute("name", "VM_PARAMETERS")
                                            .setAttribute("value", "-classpath $PROJECT_DIR$/outputs;/qub/qub/test-java/versions/2/test-java.jar;/qub/a/b/versions/1/b.jar")))))
                            .toString(XMLFormat.pretty),
                        folder.getFileContentsAsString(".idea/workspace.xml").await());
                });
            });
        });
    }
}