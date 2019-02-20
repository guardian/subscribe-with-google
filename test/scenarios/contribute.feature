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
    Then the contribution should be associated with my account

  Scenario: Ophan analytics
    When I make a contribution
    Then the contribution should be recorded in Ophan analytics

  Scenario: Tracking a contribution
     When I make a contribution
     Then the contribution should be found in the database
     
  Scenario: Seeing my entitlements
     When I have made a contribution
     Then I can see my entitlements (Guardian subscription) in Google News
     
  Scenario: Email comms from the Play store
     When I have made a contribution
     Then I should receive an email from the Play Store
     
