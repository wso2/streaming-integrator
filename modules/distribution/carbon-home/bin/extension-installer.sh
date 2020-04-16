#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Details of the SI server.
SI_SERVER_HOST="0.0.0.0"
SI_SERVER_PORT=9090

# Available Commands.
STATUS_COMMAND="status"
INSTALL_COMMAND="install"
UNINSTALL_COMMAND="uninstall"
DEPENDENCY_FLAG="-d"

get_endpoint() {
  echo "$SI_SERVER_HOST:$SI_SERVER_PORT/siddhi-extensions"
}

notify_manually_installable_dependencies() {
  response=$1
  has_manually_install=$(echo "$response" | jq 'has("manuallyInstall")')
  if $has_manually_install; then
    # Extension contains manually installable dependencies.
    manually_installable_dependencies=$(echo "$response" | jq '.manuallyInstall[]')
    echo "(*) The following dependencies should be manually installed:"
    echo "$manually_installable_dependencies" | jq '. | "  - \(.name): \(.download.instructions) "'
  fi
}

list_status() {
  if [ $# -gt 3 ]; then
    echo "Too many arguments. Arguments should be present in one of the following formats:"
    echo "$STATUS_COMMAND"
    echo "$STATUS_COMMAND [extension_name]"
    echo "$STATUS_COMMAND [extension_name] $DEPENDENCY_FLAG"
    return
  fi

  if [ -z ${2+x} ]; then
    # Extension name has not been specified. Retrieve all extension statuses.
    url="$(get_endpoint)/status"
    response="$(curl -s --location --request GET $url)"

    echo "Statuses of all Extensions"
    echo "(*) - Extension has manually installable dependencies"
    # Iterate extension names (keys) in the response.
    for extension_name in $(echo "$response" | jq '. | keys[]'); do
      has_manually_install=$(echo "$response" | jq $(echo ".$extension_name") | jq 'has("manuallyInstall")')
      if $has_manually_install; then
        # Extension contains manually installable dependencies.
        echo "$response" | jq $(echo ".$extension_name") |
          jq '. | " (*) \(.extensionInfo.name) (\(.extensionInfo.displayName) \(.extensionInfo.version)): \(.extensionStatus) "'
      else
        # Extension does not contain manually installable dependencies.
        echo "$response" | jq $(echo ".$extension_name") |
          jq '. | " - \(.extensionInfo.name) (\(.extensionInfo.displayName) \(.extensionInfo.version)): \(.extensionStatus) "'
      fi
    done
  else
    # Extension name has been specified. Retrieve the specific extension's status.
    if [ -z ${3+x} ]; then
      # $DEPENDENCY_FLAG is not present. Retrieve complete status of the given extension.
      extension_name=$2
      url="$(get_endpoint)/status/$extension_name"
      response="$(curl -s --location --request GET $url)"

      echo "Status of Extension: $extension_name"
      echo "$response" |
        jq '. | " \(.extensionInfo.name) (\(.extensionInfo.displayName) \(.extensionInfo.version)): \(.extensionStatus) "'
      notify_manually_installable_dependencies "$response"
    else
      # $DEPENDENCY_FLAG is present. Retrieve status of each dependency, of the extension.
      if [ "$3" == $DEPENDENCY_FLAG ]; then
        extension_name=$2
        echo "Dependency Statuses of Extension: $extension_name"
        url="$(get_endpoint)/status/$extension_name/dependencies"
        curl -s --location --request GET $url | jq '.'
      else
        echo "Unknown argument $3. Only $DEPENDENCY_FLAG is allowed."
      fi
    fi
  fi
}

notify_failures() {
  response=$1
  has_failures=$(echo "$response" | jq 'has("failed")')
  if $has_failures; then
    echo "Failure occurred with the following dependencies:"
    echo "$response" | jq '.failed'
  fi
}

install_extension() {
  if [ $# -eq 2 ]; then
    echo "Installing: $2"
    url="$(get_endpoint)/$2/install"
    response="$(curl -s --location --request POST $url)"
    echo "Installation finished for extension: $2, with status: $(echo "$response" | jq '.status')."
    notify_failures "$response"
    notify_manually_installable_dependencies "$response"
    echo "Please restart the server."
  else
    echo "Exactly one extension name is required."
  fi
}

handle_un_installation() {
  extension_name=$1
  echo "Un-installing: $extension_name"
  url="$(get_endpoint)/$extension_name/uninstall"
  response="$(curl -s --location --request POST $url)"
  echo "Un-installation finished for extension: $extension_name, with status: $(echo "$response" | jq '.status')."
  notify_failures "$response"
  echo "Please restart the server."
}

notify_dependency_sharing_extensions() {
  dependency_sharing_extensions_response=$1
  extension_name=$2
  echo "Extension: $extension_name shares its dependencies with the following extensions:"
  echo "$dependency_sharing_extensions_response" | jq '.sharesWith | keys[]'

  # Get confirmation from the user.
  read -r -p "You might need to re-install these extensions. Are you sure you want to un-install? [y/N] " choice
  case "$choice" in
  [yY][eE][sS] | [yY])
    handle_un_installation "$extension_name"
    ;;
  *)
    return
    ;;
  esac
}

un_install_extension() {
  if [ $# -eq 2 ]; then
    extension_name=$2
    # Check for dependency sharing extensions.
    url="$(get_endpoint)/$extension_name/dependency-sharing-extensions"
    response="$(curl -s --location --request GET $url)"

    does_share_dependencies=$(echo "$response" | jq '.doesShareDependencies')
    if $does_share_dependencies; then
      notify_dependency_sharing_extensions "$response" "$extension_name"
    else
      handle_un_installation "$extension_name"
    fi
  else
    echo "Exactly one extension name is required."
  fi
}

display_help() {
  echo "Available Commands:"
  echo " $STATUS_COMMAND"
  echo "    $STATUS_COMMAND - List down statuses of all the extensions."
  echo "    $STATUS_COMMAND [extension_name] - List down the complete status of the given extension."
  echo "    $STATUS_COMMAND [extension_name] $DEPENDENCY_FLAG - List down dependency statuses of the given extension."
  echo " $INSTALL_COMMAND"
  echo "    $INSTALL_COMMAND [extension_name] - Install the given extension."
  echo " $UNINSTALL_COMMAND"
  echo "    $UNINSTALL_COMMAND [extension_name] - Un-install the given extension."
}

execute_command() {
  # Make sure that curl is available.
  if ! type curl >/dev/null; then
    echo "Please install curl."
    return
  fi

  # Make sure that jq is available.
  if ! type jq >/dev/null; then
    echo "Please install jq."
    return
  fi

  # Execute functions based on the given command.
  case $1 in
  $STATUS_COMMAND)
    list_status "$@"
    ;;
  $INSTALL_COMMAND)
    install_extension "$@"
    ;;
  $UNINSTALL_COMMAND)
    un_install_extension "$@"
    ;;
  *)
    echo "Unknown command: $1"
    display_help
    ;;
  esac
}

execute_command "$@"
