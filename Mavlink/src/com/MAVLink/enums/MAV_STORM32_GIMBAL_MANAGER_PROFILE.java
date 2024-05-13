/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

package com.MAVLink.enums;

/**
 * Gimbal manager profiles. Only standard profiles are defined. Any implementation can define it's own profile in addition, and should use enum values > 16.
 */
public class MAV_STORM32_GIMBAL_MANAGER_PROFILE {
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_DEFAULT = 0; /* Default profile. Implementation specific. | */
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_CUSTOM = 1; /* Custom profile. Configurable profile according to the STorM32 definition. Is configured with STORM32_GIMBAL_MANAGER_PROFIL. | */
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_COOPERATIVE = 2; /* Default cooperative profile. Uses STorM32 custom profile with default settings to achieve cooperative behavior. | */
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_EXCLUSIVE = 3; /* Default exclusive profile. Uses STorM32 custom profile with default settings to achieve exclusive behavior. | */
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_PRIORITY_COOPERATIVE = 4; /* Default priority profile with cooperative behavior for equal priority. Uses STorM32 custom profile with default settings to achieve priority-based behavior. | */
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_PRIORITY_EXCLUSIVE = 5; /* Default priority profile with exclusive behavior for equal priority. Uses STorM32 custom profile with default settings to achieve priority-based behavior. | */
   public static final int MAV_STORM32_GIMBAL_MANAGER_PROFILE_ENUM_END = 6; /*  | */
}