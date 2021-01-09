package qub;

public interface QubDependenciesTests
{
    static void test(TestRunner runner)
    {
        PreCondition.assertNotNull(runner, "runner");

        runner.testGroup(QubDependencies.class, () ->
        {
            runner.testGroup("main(String[])", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependencies.main(null),
                        new PreConditionFailure("args cannot be null."));
                });
            });

            runner.testGroup("run(FakeDesktopProcess)", () ->
            {
                runner.test("with null", (Test test) ->
                {
                    test.assertThrows(() -> QubDependencies.run(null),
                        new PreConditionFailure("process cannot be null."));
                });

                runner.test("with no action", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create())
                    {
                        QubDependencies.run(process);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-dependencies [--action=]<action-name> [--help]",
                                "  Perform operations on the dependencies of a project source folder.",
                                "  --action(a): The name of the action to invoke.",
                                "  --help(?):   Show the help message for this application.",
                                "",
                                "Actions:",
                                "  list:   List the dependencies of a project.",
                                "  update: Update the dependencies of a project."),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                        test.assertEqual(-1, process.getExitCode());
                    }
                });

                runner.test("with -?", (Test test) ->
                {
                    try (final FakeDesktopProcess process = FakeDesktopProcess.create("-?"))
                    {
                        QubDependencies.run(process);

                        test.assertEqual(
                            Iterable.create(
                                "Usage: qub-dependencies [--action=]<action-name> [--help]",
                                "  Perform operations on the dependencies of a project source folder.",
                                "  --action(a): The name of the action to invoke.",
                                "  --help(?):   Show the help message for this application.",
                                "",
                                "Actions:",
                                "  list:   List the dependencies of a project.",
                                "  update: Update the dependencies of a project."),
                            Strings.getLines(process.getOutputWriteStream().getText().await()));
                        test.assertEqual(-1, process.getExitCode());
                    }
                });
            });
        });
    }
}
