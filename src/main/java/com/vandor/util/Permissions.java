package com.vandor.util;

import com.vandor.exceptions.AccessDeniedException;
import com.vandor.models.User;
import com.vandor.models.UserRole;
import com.vandor.models.UserStatus;

public final class Permissions {
  private Permissions() {}

  public static void require(User user, UserRole role) {
    if (user == null || user.getStatus() != UserStatus.ACTIVE || user.getRole() != role) {
      throw new AccessDeniedException();
    }
  }
}
