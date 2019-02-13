Feature: Contribute with Google
  In order to make a quick, frictionless contribution to The Guardian
  As a Guardian reader
  I want to make a contribution with Contribute with Google

  Scenario: Contribution thank you e-mail
    When I make a contribution
    Then I should receive a thank you e-mail

  Scenario: New user contribution
    Given I do not have a Guardian account
    When I make a contribution
    Then A Guardian guest account should be created for me

  Scenario: Existing user contribution
    Given I have a Guardian account
    When I make a contribution
    Then I the contribution should be associated with my account

  Scenario: Ophan analytics
    When I make a contribution
    Then I contribution should be recorded in Ophan analytics
