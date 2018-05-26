// File: codenarcRules.groovy
// Most of all skips should be fixed in the code

ruleset {
  ruleset('rulesets/basic.xml')
  ruleset('rulesets/braces.xml')
  ruleset('rulesets/concurrency.xml')
  ruleset('rulesets/convention.xml') {
    // Don't need due to code readablilty
    NoDef(enabled:false)
    // TBD
    CouldBeElvis(enabled:false)
  }
  ruleset('rulesets/design.xml') {
    // Don't need due to code readablilty
    BuilderMethodWithSideEffects(enabled:false)
    // Sometimes nested loop is cleaner than extracting a new method
    NestedForLoop(enabled:false)
    // TBD
    // ImplementationAsType(enabled:false)
  }
  ruleset('rulesets/dry.xml') {
    // Not necessarily an issue
    DuplicateStringLiteral(enabled:false)
  }
//  Raises a lot of "Compilation failed" warnings
//  ruleset('rulesets/enhanced.xml')
  ruleset('rulesets/exceptions.xml'){
    // Not necessarily an issue
    CatchException(enabled:false)
    // Not necessarily an issue
    ThrowRuntimeException(enabled:false)
  }
  ruleset('rulesets/formatting.xml'){
    // Don't need due to code readablilty
    ConsecutiveBlankLines(enabled:false)
    // TBD: Causes false positive alerts
    // SpaceAfterClosingBrace(enabled:false)
    // SpaceBeforeOpeningBrace(enabled:false)
    // Enforce at least one space after map entry colon
    SpaceAroundMapEntryColon {
            characterAfterColonRegex = /\s/
            characterBeforeColonRegex = /./
    }
  }
  ruleset('rulesets/generic.xml')
  ruleset('rulesets/grails.xml')
  ruleset('rulesets/groovyism.xml'){
    // Not necessarily an issue
    GStringExpressionWithinString(enabled:false)
  }
  //ruleset('rulesets/imports.xml')
  ruleset('rulesets/jdbc.xml')
  ruleset('rulesets/junit.xml')
  ruleset('rulesets/logging.xml'){
    // Can't be used in jenklins pipelines
    Println(enabled:false)
  }
  ruleset('rulesets/naming.xml'){
    // Don't need due to code readablilty
    FactoryMethodName(enabled:false)
    // Don't need due to code readablilty
    VariableName(enabled:false)
  }
  ruleset('rulesets/security.xml'){
    // Don't need to satisfy the Java Beans specification
    JavaIoPackageAccess(enabled:false)
  }
  ruleset('rulesets/serialization.xml')
  ruleset('rulesets/size.xml'){
    // TBD
    AbcMetric(enabled:false)
    // TBD
    MethodSize(enabled:false)
    // TBD
    NestedBlockDepth(enabled:false)
    // Not necessarily an issue
    ParameterCount(enabled:false)
    // TBD
    CyclomaticComplexity(enabled:false)
  }
  ruleset('rulesets/unnecessary.xml'){
    // Don't need due to code readablilty
    UnnecessaryDefInVariableDeclaration(enabled:false)
    // Not necessarily an issue
    UnnecessaryGetter(enabled:false)
    // Not necessarily an issue
    UnnecessaryReturnKeyword(enabled:false)
  }
  ruleset('rulesets/unused.xml')
}
