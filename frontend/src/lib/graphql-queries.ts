import { gql } from 'graphql-request';

/**
 * GraphQL query to get employee profile by user ID
 */
export const GET_PROFILE_QUERY = gql`
  query GetProfile($userId: UUID!) {
    profile(userId: $userId) {
      userId
      legalFirstName
      legalLastName
      preferredName
      email
      employeeId
      department
      jobCode
      jobFamily
      jobLevel
      employmentStatus
      hireDate
      terminationDate
      fte
      jobTitle
      officeLocation
      workPhone
      workLocationType
      bio
      skills
      profilePhotoUrl
      personalEmail
      personalPhone
      homeAddress
      emergencyContactName
      emergencyContactPhone
      emergencyContactRelationship
      dateOfBirth
      visaWorkPermit
      salary
      performanceRating
      createdAt
      updatedAt
      metadata {
        relationship
        visibleFields
        editableFields
      }
    }
  }
`;

/**
 * GraphQL query to get feedback for a specific user
 */
export const GET_FEEDBACK_FOR_USER_QUERY = gql`
  query GetFeedbackForUser($userId: UUID!) {
    feedbackForUser(userId: $userId) {
      id
      text
      aiPolished
      createdAt
      author {
        id
        email
        employeeId
        profile {
          preferredName
          legalFirstName
          legalLastName
        }
      }
      recipient {
        id
        email
        employeeId
        profile {
          preferredName
          legalFirstName
          legalLastName
        }
      }
    }
  }
`;

/**
 * GraphQL query to get feedback authored by current user
 */
export const GET_MY_AUTHORED_FEEDBACK_QUERY = gql`
  query GetMyAuthoredFeedback {
    myAuthoredFeedback {
      id
      text
      aiPolished
      createdAt
      recipient {
        id
        email
        employeeId
        profile {
          preferredName
          legalFirstName
          legalLastName
        }
      }
    }
  }
`;

/**
 * GraphQL query to get feedback received by current user
 */
export const GET_MY_RECEIVED_FEEDBACK_QUERY = gql`
  query GetMyReceivedFeedback {
    myReceivedFeedback {
      id
      text
      aiPolished
      createdAt
      author {
        id
        email
        employeeId
        profile {
          preferredName
          legalFirstName
          legalLastName
        }
      }
    }
  }
`;
