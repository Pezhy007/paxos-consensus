#!/bin/bash

# Complete setup, compilation, and test script for Paxos implementation

echo "===== Paxos Consensus Implementation Setup ====="
echo ""

# Function to create a Java file
create_java_file() {
    local filepath=$1
    local package=$2
    shift 2
    local content="$@"
    
    mkdir -p $(dirname "$filepath")
    echo "package $package;" > "$filepath"
    echo "" >> "$filepath"
    echo "$content" >> "$filepath"
    echo "Created: $filepath"
}

# Step 1: Verify directory structure
echo "Step 1: Creating directory structure..."
mkdir -p src/main/java/paxos/messages
mkdir -p src/main/java/paxos/network
mkdir -p src/main/java/paxos/roles
mkdir -p config
mkdir -p logs
mkdir -p build
echo "✓ Directories created"
echo ""

# Step 2: Create configuration file
echo "Step 2: Creating network configuration..."
cat > config/network.config << 'EOF'
# Network configuration for council members
# Format: MemberID,Hostname,Port
M1,localhost,9001
M2,localhost,9002
M3,localhost,9003
M4,localhost,9004
M5,localhost,9005
M6,localhost,9006
M7,localhost,9007
M8,localhost,9008
M9,localhost,9009
EOF
echo "✓ config/network.config created"
echo ""

# Step 3: Copy Java files
echo "Step 3: Creating Java source files..."
echo ""
echo "IMPORTANT: Now you need to copy the Java code from the artifacts above"
echo "into these files:"
echo ""
echo "  1. src/main/java/paxos/messages/ProposalNumber.java"
echo "  2. src/main/java/paxos/messages/PaxosMessage.java"
echo "  3. src/main/java/paxos/network/MemberInfo.java"
echo "  4. src/main/java/paxos/network/NetworkConfig.java"
echo "  5. src/main/java/paxos/network/NetworkHandler.java"
echo "  6. src/main/java/paxos/roles/Proposer.java"
echo "  7. src/main/java/paxos/roles/Acceptor.java"
echo "  8. src/main/java/paxos/roles/Learner.java"
echo "  9. src/main/java/paxos/CouncilMember.java"
echo ""
echo "Press Enter after you've copied all files..."
read

# Step 4: Verify all files exist
echo "Step 4: Verifying files..."
FILES=(
    "src/main/java/paxos/messages/ProposalNumber.java"
    "src/main/java/paxos/messages/PaxosMessage.java"
    "src/main/java/paxos/network/MemberInfo.java"
    "src/main/java/paxos/network/NetworkConfig.java"
    "src/main/java/paxos/network/NetworkHandler.java"
    "src/main/java/paxos/roles/Acceptor.java"
    "src/main/java/paxos/roles/Proposer.java"
    "src/main/java/paxos/roles/Learner.java"
    "src/main/java/paxos/CouncilMember.java"
)

all_exist=true
for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ Missing: $file"
        all_exist=false
    fi
done

if [ "$all_exist" = false ]; then
    echo ""
    echo "Some files are missing. Please create them and run this script again."
    exit 1
fi
echo ""

# Step 5: Compile
echo "Step 5: Compiling Java files..."
rm -rf build
mkdir build

# Compile in dependency order
echo "Compiling messages package..."
javac -d build src/main/java/paxos/messages/ProposalNumber.java src/main/java/paxos/messages/PaxosMessage.java
if [ $? -ne 0 ]; then
    echo "✗ Failed to compile messages package"
    exit 1
fi

echo "Compiling network package..."
javac -cp build -d build src/main/java/paxos/network/MemberInfo.java
javac -cp build -d build src/main/java/paxos/network/NetworkConfig.java
javac -cp build -d build src/main/java/paxos/network/NetworkHandler.java
if [ $? -ne 0 ]; then
    echo "✗ Failed to compile network package"
    exit 1
fi

echo "Compiling roles package..."
javac -cp build -d build src/main/java/paxos/roles/Acceptor.java
javac -cp build -d build src/main/java/paxos/roles/Proposer.java
javac -cp build -d build src/main/java/paxos/roles/Learner.java
if [ $? -ne 0 ]; then
    echo "✗ Failed to compile roles package"
    exit 1
fi

echo "Compiling main class..."
javac -cp build -d build src/main/java/paxos/CouncilMember.java
if [ $? -ne 0 ]; then
    echo "✗ Failed to compile CouncilMember"
    exit 1
fi

echo "✓ Compilation successful!"
echo ""

# Step 6: Run test
echo "Step 6: Running test..."
echo ""
echo "Choose test option:"
echo "1. Automated test (runs in background)"
echo "2. Interactive test (opens 5 terminals)"
echo "3. Skip test"
echo ""
read -p "Choice (1-3): " choice

case $choice in
    1)
        echo "Running automated test..."
        
        # Kill any existing processes
        pkill -f "paxos.CouncilMember" 2>/dev/null
        sleep 1
        
        # Start 5 members
        for i in {1..5}; do
            java -cp build paxos.CouncilMember M$i --profile reliable > logs/M$i.log 2>&1 &
            echo "Started M$i (PID: $!)"
        done
        
        echo "Waiting for initialization..."
        sleep 3
        
        echo "M1 proposing M5..."
        echo "M5" | java -cp build paxos.CouncilMember M1 --profile reliable > logs/proposal.log 2>&1 &
        
        sleep 5
        
        echo ""
        echo "Test Results:"
        grep -h "CONSENSUS:" logs/*.log 2>/dev/null | head -1
        
        echo ""
        echo "Cleaning up..."
        pkill -f "paxos.CouncilMember"
        ;;
        
    2)
        echo "Starting interactive test..."
        pkill -f "paxos.CouncilMember" 2>/dev/null
        
        for i in {1..5}; do
            # For Mac
            osascript -e "tell app \"Terminal\" to do script \"cd $(pwd) && java -cp build paxos.CouncilMember M$i --profile reliable\"" 2>/dev/null
            # For Linux (uncomment and comment Mac line above)
            # gnome-terminal -- bash -c "cd $(pwd) && java -cp build paxos.CouncilMember M$i --profile reliable; bash"
        done
        
        echo ""
        echo "5 terminals opened. Type a candidate name in any terminal to propose."
        ;;
        
    3)
        echo "Test skipped."
        ;;
esac

echo ""
echo "===== Setup Complete ====="
echo ""
echo "To run a member manually:"
echo "  java -cp build paxos.CouncilMember M1 --profile reliable"
echo ""
echo "To run all tests:"
echo "  ./scripts/run_tests.sh"